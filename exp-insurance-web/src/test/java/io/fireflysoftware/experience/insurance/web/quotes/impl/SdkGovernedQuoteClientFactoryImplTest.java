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

package io.fireflysoftware.experience.insurance.web.quotes.impl;

import io.fireflysoftware.domain.insurance.underwriting.sdk.invoker.auth.HttpBearerAuth;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SdkGovernedQuoteClientFactoryImplTest {
    private final SdkGovernedQuoteClientFactoryImpl factory = new SdkGovernedQuoteClientFactoryImpl(
            "https://underwriting-domain.invalid", "insurance-domain", false);

    @Test
    void concurrentCallersGetSeparateOfficialBearerClients() {
        var one = factory.current().contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                caller("synthetic-one", "insurance-domain", Instant.now().plusSeconds(60))));
        var two = factory.current().contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                caller("synthetic-two", "insurance-domain", Instant.now().plusSeconds(60))));
        StepVerifier.create(Mono.zip(one, two)).assertNext(clients -> {
            var first = clients.getT1().getApiClient();
            var second = clients.getT2().getApiClient();
            assertNotSame(first, second);
            assertEquals("synthetic-one", assertInstanceOf(HttpBearerAuth.class,
                    first.getAuthentication("governedQuoteBearer")).getBearerToken());
            assertEquals("synthetic-two", assertInstanceOf(HttpBearerAuth.class,
                    second.getAuthentication("governedQuoteBearer")).getBearerToken());
        }).verifyComplete();
    }

    @Test
    void absentOrExpiredCallerCannotAcquireADomainClient() {
        var unauthenticated = new JwtAuthenticationToken(
                caller("synthetic-unverified", "insurance-domain", Instant.now().plusSeconds(60)).getToken());
        assertFalse(unauthenticated.isAuthenticated());
        StepVerifier.create(factory.current().contextWrite(ReactiveSecurityContextHolder.withAuthentication(unauthenticated)))
                .expectErrorMatches(error -> status(error, 401)).verify();
        StepVerifier.create(factory.current()).expectErrorMatches(error -> status(error, 401)).verify();
        StepVerifier.create(factory.current().contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(new SecurityContextImpl()))))
                .expectErrorMatches(error -> status(error, 401)).verify();
        StepVerifier.create(factory.current().contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                        caller("synthetic-expired", "insurance-domain", Instant.now().minusSeconds(1)))))
                .expectErrorMatches(error -> status(error, 401)).verify();
    }

    @Test
    void anExperienceOnlyTokenCannotBeRelayedToAnotherAudience() {
        StepVerifier.create(factory.current().contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                        caller("synthetic-wrong-audience", "insurance-experience", Instant.now().plusSeconds(60)))))
                .expectErrorMatches(error -> status(error, 403)).verify();
    }

    @Test
    void plainHttpIsAvailableOnlyForExplicitLoopbackFixtures() {
        assertFalse(SdkGovernedQuoteClientFactoryImpl.validEndpoint("http://underwriting.invalid", true));
        assertFalse(SdkGovernedQuoteClientFactoryImpl.validEndpoint("http://127.0.0.1:18080", false));
        assertTrue(SdkGovernedQuoteClientFactoryImpl.validEndpoint("http://127.0.0.1:18080", true));
        assertFalse(SdkGovernedQuoteClientFactoryImpl.validEndpoint("https://user:secret@example.invalid", false));
        assertFalse(SdkGovernedQuoteClientFactoryImpl.validEndpoint("https://example.invalid?token=value", false));
        assertFalse(SdkGovernedQuoteClientFactoryImpl.validEndpoint("https://example.invalid#fragment", false));
    }

    private static JwtAuthenticationToken caller(String value, String audience, Instant expiry) {
        return new JwtAuthenticationToken(Jwt.withTokenValue(value).header("alg", "RS256")
                .subject("synthetic-caller").issuedAt(expiry.minusSeconds(60)).expiresAt(expiry)
                .audience(List.of(audience)).build(), List.of());
    }

    private static boolean status(Throwable error, int expected) {
        return error instanceof ResponseStatusException response && response.getStatusCode().value() == expected;
    }
}
