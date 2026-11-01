/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.experience.insurance.web.config;

import io.fireflysoftware.experience.insurance.core.journey.quotes.GovernedQuoteClientFactory;
import io.fireflysoftware.experience.insurance.web.quotes.impl.SdkGovernedQuoteClientFactoryImpl;
import org.fireflyframework.security.rs.AccessTokenValidators;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

@Configuration(proxyBeanMethods = false)
public class GovernedInsuranceConfiguration {
    @Bean
    GovernedQuoteClientFactory governedQuoteClientFactory(
            @Value("${api-configuration.domain-platform.insurance-underwriting.base-path:}") String basePath,
            @Value("${insurance.governed.underwriting-audience:}") String audience,
            @Value("${insurance.governed.allow-loopback:false}") boolean loopback) {
        return new SdkGovernedQuoteClientFactoryImpl(basePath, audience, loopback);
    }

    @Bean
    @Order(-100)
    SecurityWebFilterChain governedInsuranceSecurity(ServerHttpSecurity http,
            @Value("${insurance.governed.security.issuer:}") String issuer,
            @Value("${insurance.governed.security.audience:}") String audience,
            @Value("${insurance.governed.security.jwk-set-uri:}") String jwks,
            @Value("${insurance.governed.allow-loopback:false}") boolean loopback) {
        ReactiveJwtDecoder decoder;
        if (!SdkGovernedQuoteClientFactoryImpl.validEndpoint(issuer, loopback)
                || !SdkGovernedQuoteClientFactoryImpl.validEndpoint(jwks, loopback) || audience.isBlank()) {
            decoder = token -> Mono.error(new JwtException("Governed insurance trust is not configured"));
        } else {
            var configured = NimbusReactiveJwtDecoder.withJwkSetUri(jwks).build();
            configured.setJwtValidator(AccessTokenValidators.forIssuerAndAudience(issuer, audience));
            decoder = configured;
        }
        return http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                        "/api/v1/experience/insurance/tenants/**",
                        "/api/v1/experience/insurance/governed/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .authorizeExchange(exchanges -> exchanges.anyExchange().authenticated())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtDecoder(decoder)))
                .build();
    }
}
