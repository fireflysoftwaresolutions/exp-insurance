package io.fireflysoftware.experience.insurance.web.policies.impl;

import io.fireflysoftware.experience.insurance.web.policies.*;
import io.fireflysoftware.experience.insurance.core.journey.policies.GovernedPolicyDomainPort.Action;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.*;
import org.springframework.mock.http.client.reactive.*;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ExchangedGovernedPolicyDomainClientFactoryTest {
    final Instant now=Instant.parse("2027-01-01T00:00:00Z");final Clock clock=Clock.fixed(now,ZoneOffset.UTC);
    final UUID party=UUID.randomUUID();final Set<String> bind=Set.of("insurance:policies:bind");
    final GovernedPolicyDomainClientSettings settings=new GovernedPolicyDomainClientSettings("https://issuer.invalid",URI.create("https://issuer.invalid/jwks"),"experience-policy","interactive-client","opaque-platform-namespace",now.plusSeconds(60),URI.create("https://domain-policy.invalid"),"domain-policy","experience-policy-exchange","policy-exchange");
    final ReactiveJwtDecoder decoder=mock(ReactiveJwtDecoder.class);final AtomicInteger calls=new AtomicInteger();final AtomicReference<String> form=new AtomicReference<>(),reply=new AtomicReference<>();
    @BeforeEach void tokenReply(){reply.set("{\"access_token\":\"exchanged-token\",\"token_type\":\"Bearer\",\"expires_in\":30,\"scope\":\"insurance:policies:bind\",\"issued_token_type\":\"urn:ietf:params:oauth:token-type:access_token\"}");when(decoder.decode("exchanged-token")).thenReturn(Mono.just(jwt("exchanged-token","HUMAN","opaque subject","domain-policy","experience-policy-exchange",party,"opaque-platform-namespace",now.plusSeconds(30),bind)));}
    Jwt jwt(String token,String kind,String subject,String audience,String client,UUID signedParty,String tenant,Instant expiry,Set<String> scopes){
        return Jwt.withTokenValue(token).header("alg","RS256").issuer(settings.issuer()).subject(subject).audience(List.of(audience)).issuedAt(now.minusSeconds(1)).notBefore(now.minusSeconds(1)).expiresAt(expiry)
                .claim("client_id",client).claim("principal_kind",kind).claim("party_id",signedParty.toString()).claim("tenant_id",tenant).claim("scope",String.join(" ",scopes)).build();
    }
    Jwt original(){return jwt("original-subject-token","HUMAN","opaque subject","experience-policy","interactive-client",party,"opaque-platform-namespace",now.plusSeconds(60),bind);}
    JwtAuthenticationToken auth(Jwt jwt){return new JwtAuthenticationToken(jwt,List.of());}
    <T> Mono<T> context(Mono<T> work,JwtAuthenticationToken auth){return work.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));}
    ClientRegistration registration(AuthorizationGrantType grant){return ClientRegistration.withRegistrationId("policy-exchange").clientId("experience-policy-exchange").clientSecret("synthetic-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(grant).tokenUri("https://issuer.invalid/oauth/token").issuerUri(settings.issuer()).scope(Set.of("insurance:policies:bind","insurance:policies:issue","insurance:policies:recover","insurance:policies:read")).build();}
    ClientHttpConnector connector(){return (method,uri,callback)->{
        calls.incrementAndGet();var request=new MockClientHttpRequest(method,uri);return callback.apply(request).then(Mono.defer(request::getBodyAsString)).map(body->{form.set(body);var response=new MockClientHttpResponse(HttpStatus.OK);response.getHeaders().setContentType(MediaType.APPLICATION_JSON);response.setBody(reply.get());return (ClientHttpResponse)response;});
    };}
    ExchangedGovernedPolicyDomainClientFactoryImpl factory(){return new ExchangedGovernedPolicyDomainClientFactoryImpl(settings,id->Mono.just(registration(AuthorizationGrantType.TOKEN_EXCHANGE)),decoder,connector(),clock,System::nanoTime);}
    @Test void actualNativeExchangeCarriesOriginalSubjectAndReturnsACallerOwnedLease(){
        StepVerifier.create(context(factory().current(Action.BIND),auth(original()))).assertNext(client->{assertNotNull(client.connector());assertEquals(settings.endpoint(),client.endpoint());assertTrue(client.remaining().compareTo(Duration.ofSeconds(5))<=0);assertEquals("exchanged-token",client.bearerToken());assertFalse(client.toString().contains("exchanged-token"));}).verifyComplete();
        assertEquals(1,calls.get());assertTrue(form.get().contains("subject_token=original-subject-token"));assertTrue(form.get().contains("audience=domain-policy"));assertFalse(form.get().contains("actor_token="));
    }
    @Test void missingNativeVerifiedContextCannotStartAnExchange(){StepVerifier.create(factory().current(Action.BIND)).expectErrorSatisfies(error->assertEquals(HttpStatus.UNAUTHORIZED,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(0,calls.get());verifyNoInteractions(decoder);}
    @Test void aClaimedJwtCannotReplaceAnAuthenticatedNativeJwt(){var authentication=auth(original());authentication.setAuthenticated(false);StepVerifier.create(context(factory().current(Action.BIND),authentication)).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());}
    @Test void staffCannotBorrowTheHumanProfile(){var staff=jwt("staff","STAFF","opaque subject","experience-policy","interactive-client",party,"opaque-platform-namespace",now.plusSeconds(60),bind);StepVerifier.create(context(factory().current(Action.BIND),auth(staff))).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());}
    @Test void wrongOriginalAudienceClientOrTenantRefusesBeforeExchange(){
        for(var jwt:List.of(jwt("wrong-aud","HUMAN","opaque subject","wrong","interactive-client",party,"opaque-platform-namespace",now.plusSeconds(60),bind),jwt("wrong-client","HUMAN","opaque subject","experience-policy","wrong",party,"opaque-platform-namespace",now.plusSeconds(60),bind),jwt("wrong-tenant","HUMAN","opaque subject","experience-policy","interactive-client",party,"another-namespace",now.plusSeconds(60),bind))){StepVerifier.create(context(factory().current(Action.BIND),auth(jwt))).expectError(ResponseStatusException.class).verify();}
        assertEquals(0,calls.get());verifyNoInteractions(decoder);
    }
    @Test void anotherReturnedSubjectIsRefusedBeforeClientPublication(){when(decoder.decode("exchanged-token")).thenReturn(Mono.just(jwt("exchanged-token","HUMAN","another subject","domain-policy","experience-policy-exchange",party,"opaque-platform-namespace",now.plusSeconds(30),bind)));StepVerifier.create(context(factory().current(Action.BIND),auth(original()))).expectError(ResponseStatusException.class).verify();assertEquals(1,calls.get());}
    @Test void anotherReturnedAudienceOrPartyCannotPublishAClient(){
        for(var target:List.of(jwt("exchanged-token","HUMAN","opaque subject","another-audience","experience-policy-exchange",party,"opaque-platform-namespace",now.plusSeconds(30),bind),jwt("exchanged-token","HUMAN","opaque subject","domain-policy","experience-policy-exchange",UUID.randomUUID(),"opaque-platform-namespace",now.plusSeconds(30),bind))){when(decoder.decode("exchanged-token")).thenReturn(Mono.just(target));StepVerifier.create(context(factory().current(Action.BIND),auth(original()))).expectError(ResponseStatusException.class).verify();}assertEquals(2,calls.get());
    }
    @Test void missingIssuedTokenTypeIsRefusedByProductionExtractor(){reply.set(reply.get().replace(",\"issued_token_type\":\"urn:ietf:params:oauth:token-type:access_token\"",""));StepVerifier.create(context(factory().current(Action.BIND),auth(original()))).expectError(ResponseStatusException.class).verify();assertEquals(1,calls.get());verifyNoInteractions(decoder);}
    @Test void emptyNativeDecoderCannotBecomeSuccessfulAcquisition(){when(decoder.decode(anyString())).thenReturn(Mono.empty());StepVerifier.create(context(factory().current(Action.BIND),auth(original()))).expectError(ResponseStatusException.class).verify();}
    @Test void clientCredentialsCannotReplaceTheHumanSubjectGrant(){var factory=new ExchangedGovernedPolicyDomainClientFactoryImpl(settings,id->Mono.just(registration(AuthorizationGrantType.CLIENT_CREDENTIALS)),decoder,connector(),clock,System::nanoTime);StepVerifier.create(context(factory.current(Action.BIND),auth(original()))).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());}
    @Test void anAcquiredHandleRejectsAChangedNativeCaller(){
        var first=auth(original());var other=auth(jwt("other","HUMAN","other subject","experience-policy","interactive-client",UUID.randomUUID(),"opaque-platform-namespace",now.plusSeconds(60),bind));
        StepVerifier.create(context(factory().current(Action.BIND).flatMap(client->context(client.revalidate(),other)),first)).expectError(ResponseStatusException.class).verify();assertEquals(1,calls.get());
    }
    @Test void aWallClockRollbackCannotExtendTheOriginalExchangedTokenWindow(){
        var wall=new AtomicReference<>(now);var monotonic=new AtomicLong();var movingClock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return wall.get();}};
        when(decoder.decode("exchanged-token")).thenReturn(Mono.just(jwt("exchanged-token","HUMAN","opaque subject","domain-policy","experience-policy-exchange",party,"opaque-platform-namespace",now.plusMillis(500),bind)));
        var factory=new ExchangedGovernedPolicyDomainClientFactoryImpl(settings,id->Mono.just(registration(AuthorizationGrantType.TOKEN_EXCHANGE)),decoder,connector(),movingClock,monotonic::get);
        StepVerifier.create(context(factory.current(Action.BIND),auth(original()))).assertNext(client->{monotonic.set(Duration.ofMillis(600).toNanos());wall.set(now.minusSeconds(60));assertThrows(ResponseStatusException.class,client::remaining);}).verifyComplete();assertEquals(1,calls.get());
    }
    @Test void anAdditionalOriginalAudienceCannotBorrowTheExactExperienceProfile(){var bad=Jwt.withTokenValue("multi").headers(h->h.putAll(original().getHeaders())).claims(c->c.putAll(original().getClaims())).audience(List.of("experience-policy","domain-policy")).build();StepVerifier.create(context(factory().current(Action.BIND),auth(bad))).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());}
    @Test void aTargetWithAdditionalScopesCannotPublishALease(){when(decoder.decode("exchanged-token")).thenReturn(Mono.just(jwt("exchanged-token","HUMAN","opaque subject","domain-policy","experience-policy-exchange",party,"opaque-platform-namespace",now.plusSeconds(30),Set.of("insurance:policies:bind","insurance:policies:issue"))));StepVerifier.create(context(factory().current(Action.BIND),auth(original()))).expectError(ResponseStatusException.class).verify();assertEquals(1,calls.get());}
    @Test void expiredAndFutureNativeHumansNeverStartAnExchange(){var expired=jwt("expired","HUMAN","opaque subject","experience-policy","interactive-client",party,"opaque-platform-namespace",now,bind);var future=Jwt.withTokenValue("future").headers(h->h.putAll(original().getHeaders())).claims(c->c.putAll(original().getClaims())).notBefore(now.plusSeconds(1)).build();for(var token:List.of(expired,future)){StepVerifier.create(context(factory().current(Action.BIND),auth(token))).expectError(ResponseStatusException.class).verify();}assertEquals(0,calls.get());}
    @Test void tokenEndpointFailureHasNoImplicitRetryOrDecoderFallback(){reply.set("{\"error\":\"temporarily_unavailable\"}");StepVerifier.create(context(factory().current(Action.BIND),auth(original()))).expectError(ResponseStatusException.class).verify();assertEquals(1,calls.get());verifyNoInteractions(decoder);}
    @Test void recoveryUsesTheExactExistingRecoverAndReadScopes(){var scopes=Set.of("insurance:policies:recover","insurance:policies:read");reply.set("{\"access_token\":\"exchanged-token\",\"token_type\":\"Bearer\",\"expires_in\":30,\"scope\":\"insurance:policies:recover insurance:policies:read\",\"issued_token_type\":\"urn:ietf:params:oauth:token-type:access_token\"}");when(decoder.decode("exchanged-token")).thenReturn(Mono.just(jwt("exchanged-token","HUMAN","opaque subject","domain-policy","experience-policy-exchange",party,"opaque-platform-namespace",now.plusSeconds(30),scopes)));StepVerifier.create(context(factory().current(Action.RECOVER),auth(jwt("recover-original","HUMAN","opaque subject","experience-policy","interactive-client",party,"opaque-platform-namespace",now.plusSeconds(60),scopes)))).expectNextCount(1).verifyComplete();assertEquals(1,calls.get());assertTrue(form.get().contains("insurance%3Apolicies%3Arecover"));assertTrue(form.get().contains("insurance%3Apolicies%3Aread"));assertFalse(form.get().contains("bind"));}
    @Test void absentRegistrationCannotBorrowAnotherGrant(){var factory=new ExchangedGovernedPolicyDomainClientFactoryImpl(settings,id->Mono.empty(),decoder,connector(),clock,System::nanoTime);StepVerifier.create(context(factory.current(Action.BIND),auth(original()))).expectErrorSatisfies(error->assertEquals(HttpStatus.SERVICE_UNAVAILABLE,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(0,calls.get());verifyNoInteractions(decoder);}
    @Test void aTargetWithWrongClientOrTenantCannotPublishALease(){for(var target:List.of(jwt("exchanged-token","HUMAN","opaque subject","domain-policy","other-client",party,"opaque-platform-namespace",now.plusSeconds(30),bind),jwt("exchanged-token","HUMAN","opaque subject","domain-policy","experience-policy-exchange",party,"another-tenant",now.plusSeconds(30),bind))){when(decoder.decode("exchanged-token")).thenReturn(Mono.just(target));StepVerifier.create(context(factory().current(Action.BIND),auth(original()))).expectError(ResponseStatusException.class).verify();}assertEquals(2,calls.get());}
    @Test void aHumanWithoutTheCommandScopeCannotExchange(){StepVerifier.create(context(factory().current(Action.ISSUE),auth(original()))).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());verifyNoInteractions(decoder);}
    @Test void zeroPartyCannotBorrowAHumanProfile(){StepVerifier.create(context(factory().current(Action.BIND),auth(jwt("zero-party","HUMAN","opaque subject","experience-policy","interactive-client",new UUID(0,0),"opaque-platform-namespace",now.plusSeconds(60),bind)))).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());}
    @Test void evenLongLivedTokensCannotResetTheOriginalFiveSecondWindow(){var monotonic=new AtomicLong();var factory=new ExchangedGovernedPolicyDomainClientFactoryImpl(settings,id->Mono.just(registration(AuthorizationGrantType.TOKEN_EXCHANGE)),decoder,connector(),clock,monotonic::get);StepVerifier.create(context(factory.current(Action.BIND),auth(original()))).assertNext(client->{monotonic.set(Duration.ofSeconds(5).toNanos());assertThrows(ResponseStatusException.class,client::remaining);assertThrows(ResponseStatusException.class,client::bearerToken);}).verifyComplete();assertEquals(1,calls.get());}
    @Test void recoveryCannotElevateAnOriginalRecoverOnlyCallerToReadAuthority(){StepVerifier.create(context(factory().current(Action.RECOVER),auth(jwt("recover-only","HUMAN","opaque subject","experience-policy","interactive-client",party,"opaque-platform-namespace",now.plusSeconds(60),Set.of("insurance:policies:recover"))))).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());verifyNoInteractions(decoder);}
}
