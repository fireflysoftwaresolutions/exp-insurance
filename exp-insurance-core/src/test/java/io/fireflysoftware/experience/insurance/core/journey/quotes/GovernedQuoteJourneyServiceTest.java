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

package io.fireflysoftware.experience.insurance.core.journey.quotes;

import io.fireflysoftware.domain.insurance.underwriting.sdk.api.GovernedQuoteOffersApi;
import io.fireflysoftware.domain.insurance.underwriting.sdk.model.QuoteCommandResponse;
import io.fireflysoftware.domain.insurance.underwriting.sdk.model.QuoteOfferRequest;
import io.fireflysoftware.experience.insurance.core.journey.quotes.impl.GovernedQuoteJourneyServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GovernedQuoteJourneyServiceTest {
    private final GovernedQuoteScope scope = new GovernedQuoteScope(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    @Test
    void aRepeatedSubscriptionKeepsTheActionButAcquiresTheCurrentCallerAgain() {
        var api = mock(GovernedQuoteOffersApi.class);
        var calls = new AtomicInteger();
        var action = UUID.randomUUID();
        var request = new QuoteOfferRequest().actionId(action);
        var response = new QuoteCommandResponse().actionId(action).intentDigest("a".repeat(64))
                .status(QuoteCommandResponse.StatusEnum.OUTCOME_UNKNOWN);
        when(api.offer(scope.tenantId(), scope.submissionId(), scope.partyId(), scope.productId(), request))
                .thenReturn(Mono.just(response));
        var service = new GovernedQuoteJourneyServiceImpl(() -> Mono.defer(() -> {
            calls.incrementAndGet();
            return Mono.just(api);
        }));

        var operation = service.offer(scope, request);
        StepVerifier.create(operation).assertNext(value -> assertSame(response, value)).verifyComplete();
        StepVerifier.create(operation).assertNext(value -> assertSame(response, value)).verifyComplete();
        assertEquals(2, calls.get());
        assertEquals(action, request.getActionId());
        verify(api, times(2)).offer(scope.tenantId(), scope.submissionId(), scope.partyId(), scope.productId(), request);
    }

    @Test
    void admissionFailureNeverInvokesTheDomain() {
        var denied = new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        var service = new GovernedQuoteJourneyServiceImpl(() -> Mono.error(denied));
        StepVerifier.create(service.offer(scope, new QuoteOfferRequest().actionId(UUID.randomUUID())))
                .expectErrorMatches(error -> error == denied).verify();
    }

    @Test
    void aDifferentActionInTheDomainReplyCannotBecomeTheRequestedOffer() {
        var api = mock(GovernedQuoteOffersApi.class);
        var request = new QuoteOfferRequest().actionId(UUID.randomUUID());
        when(api.offer(scope.tenantId(), scope.submissionId(), scope.partyId(), scope.productId(), request))
                .thenReturn(Mono.just(new QuoteCommandResponse().actionId(UUID.randomUUID())
                        .intentDigest("b".repeat(64)).status(QuoteCommandResponse.StatusEnum.COMMITTED)));
        var service = new GovernedQuoteJourneyServiceImpl(() -> Mono.just(api));
        StepVerifier.create(service.offer(scope, request))
                .expectErrorMatches(error -> error instanceof ResponseStatusException status
                        && status.getStatusCode().value() == 502).verify();
    }

    @Test
    void anEmptyDomainReplyIsNotAnEmptySuccessfulOperation() {
        var api = mock(GovernedQuoteOffersApi.class);
        var request = new QuoteOfferRequest().actionId(UUID.randomUUID());
        when(api.offer(scope.tenantId(), scope.submissionId(), scope.partyId(), scope.productId(), request))
                .thenReturn(Mono.empty());
        var service = new GovernedQuoteJourneyServiceImpl(() -> Mono.just(api));
        StepVerifier.create(service.offer(scope, request))
                .expectErrorMatches(error -> error instanceof ResponseStatusException status
                        && status.getStatusCode().value() == 502).verify();
    }
}
