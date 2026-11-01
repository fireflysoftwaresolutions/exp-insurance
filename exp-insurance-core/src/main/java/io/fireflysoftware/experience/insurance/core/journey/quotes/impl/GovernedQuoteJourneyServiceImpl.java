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

package io.fireflysoftware.experience.insurance.core.journey.quotes.impl;

import io.fireflysoftware.domain.insurance.underwriting.sdk.api.GovernedQuoteOffersApi;
import io.fireflysoftware.domain.insurance.underwriting.sdk.model.*;
import io.fireflysoftware.experience.insurance.core.journey.quotes.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Service
public final class GovernedQuoteJourneyServiceImpl implements GovernedQuoteJourneyService {
    private final GovernedQuoteClientFactory clients;

    public GovernedQuoteJourneyServiceImpl(GovernedQuoteClientFactory clients) {
        this.clients = Objects.requireNonNull(clients);
    }

    @Override
    public Mono<QuoteCommandResponse> offer(GovernedQuoteScope scope, QuoteOfferRequest request) {
        return Mono.defer(() -> invoke(api -> api.offer(scope.tenantId(), scope.submissionId(),
                        scope.partyId(), scope.productId(), request))
                .map(response -> command(response, request.getActionId())));
    }

    @Override
    public Mono<QuoteCommandResponse> accept(GovernedQuoteScope scope, UUID offerId, QuoteAcceptanceRequest request) {
        return Mono.defer(() -> invoke(api -> api.accept(scope.tenantId(), scope.submissionId(),
                        scope.partyId(), scope.productId(), offerId, request))
                .map(response -> command(response, request.getActionId())));
    }

    @Override
    public Mono<QuoteReadResponse> current(GovernedQuoteScope scope, UUID offerId) {
        return invoke(api -> api.current(scope.tenantId(), scope.submissionId(), scope.partyId(), scope.productId(), offerId));
    }

    @Override
    public Mono<QuoteHistoryResponse> history(GovernedQuoteScope scope) {
        return invoke(api -> api.history(scope.tenantId(), scope.submissionId(), scope.partyId(), scope.productId()));
    }

    @Override
    public Mono<QuoteRecoveryResponse> recover(GovernedQuoteScope scope, UUID actionId, String expectedIntentDigest) {
        return invoke(api -> api.recover(scope.tenantId(), scope.submissionId(), scope.partyId(),
                scope.productId(), actionId, expectedIntentDigest));
    }

    private <T> Mono<T> invoke(Function<GovernedQuoteOffersApi, Mono<T>> operation) {
        return Mono.defer(clients::current)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "QUOTE_DOMAIN_REQUIRED")))
                .flatMap(operation)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "EMPTY_QUOTE_DOMAIN_RESPONSE")));
    }

    private static QuoteCommandResponse command(QuoteCommandResponse response, UUID actionId) {
        if (actionId == null || !actionId.equals(response.getActionId()) || response.getStatus() == null
                || response.getIntentDigest() == null || !response.getIntentDigest().matches("[a-f0-9]{64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "QUOTE_ACTION_RESPONSE_MISMATCH");
        }
        return response;
    }
}
