package io.fireflysoftware.experience.insurance.web.policies.impl;

import io.fireflysoftware.experience.insurance.web.policies.*;
import io.fireflysoftware.experience.insurance.core.journey.policies.GovernedPolicyDomainPort.Action;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import java.util.function.LongSupplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.*;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.endpoint.*;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.DefaultMapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Native subject exchange only; each acquired handle owns a subject-exchanged lease and original deadline. */
public final class ExchangedGovernedPolicyDomainClientFactoryImpl implements GovernedPolicyDomainClientFactory {
    private final GovernedPolicyDomainClientSettings settings;
    private final ReactiveClientRegistrationRepository registrations;
    private final ReactiveJwtDecoder decoder;
    private final WebClientReactiveTokenExchangeTokenResponseClient exchange;
    private final ClientHttpConnector connector;
    private final Clock clock;
    private final LongSupplier nanos;
    public ExchangedGovernedPolicyDomainClientFactoryImpl(GovernedPolicyDomainClientSettings settings,ReactiveClientRegistrationRepository registrations){
        this(settings,registrations,decoder(settings,settings.targetAudience()),new JdkClientHttpConnector(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(2)).build()),Clock.systemUTC(),System::nanoTime);
    }
    ExchangedGovernedPolicyDomainClientFactoryImpl(GovernedPolicyDomainClientSettings settings,ReactiveClientRegistrationRepository registrations,
            ReactiveJwtDecoder decoder,ClientHttpConnector connector,Clock clock,LongSupplier nanos){
        this.settings=Objects.requireNonNull(settings);this.registrations=Objects.requireNonNull(registrations);this.decoder=Objects.requireNonNull(decoder);
        this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);
        this.connector=Objects.requireNonNull(connector);
        var tokens=WebClient.builder().clientConnector(connector).codecs(codecs->{codecs.defaultCodecs().maxInMemorySize(16384);codecs.defaultCodecs().enableLoggingRequestDetails(false);})
                .filter((request,next)->next.exchange(request).flatMap(response->response.statusCode().is2xxSuccessful()?Mono.just(response):response.releaseBody().then(Mono.error(unavailable())))).build();
        exchange=new WebClientReactiveTokenExchangeTokenResponseClient();exchange.setWebClient(tokens);
        exchange.addParametersConverter(request->{var values=new LinkedMultiValueMap<String,String>();values.set("audience",settings.targetAudience());return values;});
        var converter=new DefaultMapOAuth2AccessTokenResponseConverter();
        exchange.setBodyExtractor((message,context)->BodyExtractors.toMono(new ParameterizedTypeReference<Map<String,Object>>(){}).extract(message,new BodyExtractor.Context(){
            public List<HttpMessageReader<?>> messageReaders(){return context.messageReaders();}
            public Optional<ServerHttpResponse> serverResponse(){return context.serverResponse();}
            public Map<String,Object> hints(){var hints=new HashMap<String,Object>(context.hints());hints.put(Hints.SUPPRESS_LOGGING_HINT,true);return Map.copyOf(hints);}
        }).map(parameters->{
            if(parameters.containsKey("error") || !"urn:ietf:params:oauth:token-type:access_token".equals(parameters.get("issued_token_type")))throw unavailable();
            try{if(!com.nimbusds.oauth2.sdk.TokenResponse.parse(new net.minidev.json.JSONObject(parameters)).indicatesSuccess())throw unavailable();}
            catch(com.nimbusds.oauth2.sdk.ParseException invalid){throw unavailable();}
            return converter.convert(parameters);
        }));
    }
    @Override public Mono<Client> current(Action action){return Mono.defer(()->{
        Objects.requireNonNull(action);long started=nanos.getAsLong();Instant observedAt=clock.instant();
        Set<String> scopes=switch(action){case BIND->Set.of("insurance:policies:bind");case ISSUE->Set.of("insurance:policies:issue");case RECOVER->Set.of("insurance:policies:recover","insurance:policies:read");};
        return ReactiveSecurityContextHolder.getContext().flatMap(context->Mono.justOrEmpty(context.getAuthentication()))
                .filter(authentication->authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated()).cast(JwtAuthenticationToken.class)
                .switchIfEmpty(Mono.error(unauthorized())).flatMap(authentication->{
                    Jwt original=authentication.getToken();if(authentication.getPrincipal()!=original)return Mono.error(unauthorized());
                    original(original,scopes);remaining(started,observedAt,original.getExpiresAt());
                    return registrations.findByRegistrationId(settings.registrationId()).switchIfEmpty(Mono.error(unavailable())).flatMap(registration->{
                        registration(registration,scopes);var scoped=ClientRegistration.withClientRegistration(registration).scope(scopes).build();
                        return exchange.getTokenResponse(new TokenExchangeGrantRequest(scoped,original,null)).timeout(remaining(started,observedAt,original.getExpiresAt()))
                                .switchIfEmpty(Mono.error(unavailable())).flatMap(response->{
                                    var token=response.getAccessToken();
                                    if(token==null || !OAuth2AccessToken.TokenType.BEARER.equals(token.getTokenType()) || token.getTokenValue().isBlank() || !scopes.equals(token.getScopes()))return Mono.error(forbidden());
                                    return decoder.decode(token.getTokenValue()).timeout(remaining(started,observedAt,original.getExpiresAt())).switchIfEmpty(Mono.error(forbidden())).flatMap(target->{
                                        target(target,original,scopes);if(!token.getTokenValue().equals(target.getTokenValue()))return Mono.error(forbidden());
                                        Client client=new CallerClientImpl(authentication,original,target,scopes,started,observedAt);
                                        return client.revalidate().thenReturn(client);
                                    });
                                });
                    }).timeout(remaining(started,observedAt,original.getExpiresAt()));
                }).onErrorMap(error->error instanceof ResponseStatusException?error:unavailable());
    });}
    private final class CallerClientImpl implements Client {
        private final JwtAuthenticationToken authentication;private final Jwt original,target;
        private final Set<String> scopes;private final long started;private final Instant observedAt;
        CallerClientImpl(JwtAuthenticationToken authentication,Jwt original,Jwt target,Set<String> scopes,long started,Instant observedAt){
            this.authentication=authentication;this.original=original;this.target=target;this.scopes=scopes;this.started=started;this.observedAt=observedAt;
        }
        @Override public String bearerToken(){remaining();return target.getTokenValue();}
        @Override public ClientHttpConnector connector(){return connector;}
        @Override public URI endpoint(){return settings.endpoint();}
        @Override public Duration remaining(){return ExchangedGovernedPolicyDomainClientFactoryImpl.this.remaining(started,observedAt,target.getExpiresAt());}
        @Override public Mono<Void> revalidate(){return Mono.defer(()->{
            remaining();original(original,scopes);target(target,original,scopes);
            return ReactiveSecurityContextHolder.getContext().filter(context->context.getAuthentication()==authentication && authentication.isAuthenticated()
                    && authentication.getPrincipal()==original).switchIfEmpty(Mono.error(forbidden())).then();
        });}
        @Override public String toString(){return "GovernedPolicyCallerClient[redacted]";}
    }
    private void original(Jwt jwt,Set<String> scopes){
        valid(jwt);if(!List.of(settings.experienceAudience()).equals(jwt.getAudience()) || !settings.inboundClientId().equals(claim(jwt,"client_id"))
                || !scopes(jwt).containsAll(scopes))throw forbidden();
    }
    private void target(Jwt target,Jwt original,Set<String> scopes){
        valid(target);if(!List.of(settings.targetAudience()).equals(target.getAudience()) || !settings.delegatingClientId().equals(claim(target,"client_id"))
                || !original.getSubject().equals(target.getSubject()) || !claim(original,"party_id").equals(claim(target,"party_id"))
                || target.getExpiresAt().isAfter(original.getExpiresAt()) || target.getNotBefore().isBefore(original.getNotBefore()) || !scopes.equals(scopes(target)))throw forbidden();
    }
    private void valid(Jwt jwt){
        var now=clock.instant();String party=claim(jwt,"party_id");
        if(!settings.issuer().equals(claim(jwt,"iss")) || jwt.getSubject()==null || jwt.getSubject().isBlank() || jwt.getSubject().length()>255
                || !settings.signedTenant().equals(claim(jwt,"tenant_id")) || !"HUMAN".equals(claim(jwt,"principal_kind"))
                || !party.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") || UUID.fromString(party).equals(new UUID(0,0))
                || !(jwt.getClaims().get("exp") instanceof Instant) || !(jwt.getClaims().get("nbf") instanceof Instant)
                || jwt.getExpiresAt()==null || jwt.getNotBefore()==null || !now.isBefore(jwt.getExpiresAt()) || now.isBefore(jwt.getNotBefore())
                || !jwt.getNotBefore().isBefore(jwt.getExpiresAt()))throw forbidden();
    }
    private void registration(ClientRegistration registration,Set<String> scopes){
        URI token=URI.create(registration.getProviderDetails().getTokenUri());
        if(!"https".equals(token.getScheme()) || token.getHost()==null || token.getUserInfo()!=null || token.getQuery()!=null || token.getFragment()!=null
                || !settings.registrationId().equals(registration.getRegistrationId()) || !AuthorizationGrantType.TOKEN_EXCHANGE.equals(registration.getAuthorizationGrantType())
                || !ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(registration.getClientAuthenticationMethod()) || !settings.delegatingClientId().equals(registration.getClientId())
                || !settings.issuer().equals(registration.getProviderDetails().getIssuerUri()) || registration.getClientSecret()==null || registration.getClientSecret().isBlank()
                || !registration.getScopes().containsAll(scopes))throw forbidden();
    }
    private Duration remaining(long started,Instant observedAt,Instant expiry){
        long elapsed=nanos.getAsLong()-started;var until=expiry.isBefore(settings.notAfter())?expiry:settings.notAfter();var wall=Duration.between(clock.instant(),until);
        var originalWindow=Duration.between(observedAt,until);if(originalWindow.compareTo(Duration.ofSeconds(5))>0)originalWindow=Duration.ofSeconds(5);
        long left=originalWindow.toNanos()-elapsed;if(elapsed<0 || left<=0 || wall.isNegative() || wall.isZero())throw forbidden();
        return wall.compareTo(Duration.ofNanos(left))<0?wall:Duration.ofNanos(left);
    }
    public static ReactiveJwtDecoder decoder(GovernedPolicyDomainClientSettings settings,String audience){
        var decoder=NimbusReactiveJwtDecoder.withJwkSetUri(settings.jwkSetUri().toASCIIString()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(settings.issuer()),new JwtTimestampValidator(Duration.ZERO),
                jwt->List.of(audience).equals(jwt.getAudience())?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Exact governed policy audience required",null))));return decoder;
    }
    private static Set<String> scopes(Jwt jwt){
        Object raw=jwt.getClaims().get("scope");if(!(raw instanceof String text))throw forbidden();var scopes=Set.copyOf(Arrays.asList(text.split(" ",-1)));
        if(scopes.contains("") || jwt.getClaims().containsKey("scp"))throw forbidden();return scopes;
    }
    private static String claim(Jwt jwt,String key){Object value=jwt.getClaims().get(key);if(!(value instanceof String text) || text.isBlank() || text.codePoints().anyMatch(Character::isISOControl))throw forbidden();return text;}
    private static ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Governed policy subject exchange unavailable");}
    private static ResponseStatusException unauthorized(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Verified native governed policy caller required");}
    private static ResponseStatusException forbidden(){return new ResponseStatusException(HttpStatus.FORBIDDEN,"Current governed policy delegation refused");}
    @Override public String toString(){return "ExchangedGovernedPolicyDomainClientFactoryImpl[redacted]";}
}
