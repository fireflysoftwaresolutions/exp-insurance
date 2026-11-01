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

import io.fireflysoftware.domain.insurance.underwriting.sdk.model.*;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface GovernedQuoteJourneyService {
    Mono<QuoteCommandResponse> offer(GovernedQuoteScope scope, QuoteOfferRequest request);
    Mono<QuoteCommandResponse> accept(GovernedQuoteScope scope, UUID offerId, QuoteAcceptanceRequest request);
    Mono<QuoteReadResponse> current(GovernedQuoteScope scope, UUID offerId);
    Mono<QuoteHistoryResponse> history(GovernedQuoteScope scope);
    Mono<QuoteRecoveryResponse> recover(GovernedQuoteScope scope, UUID actionId, String expectedIntentDigest);
}
