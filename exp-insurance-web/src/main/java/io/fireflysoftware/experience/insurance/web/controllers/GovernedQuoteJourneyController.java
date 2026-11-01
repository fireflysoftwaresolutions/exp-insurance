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

package io.fireflysoftware.experience.insurance.web.controllers;

import io.fireflysoftware.domain.insurance.underwriting.sdk.model.*;
import io.fireflysoftware.experience.insurance.core.journey.quotes.GovernedQuoteJourneyService;
import io.fireflysoftware.experience.insurance.core.journey.quotes.GovernedQuoteScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/experience/insurance/tenants/{tenantId}/submissions/{submissionId}/parties/{partyId}/products/{productId}/quote-offers")
@Tag(name = "GovernedInsuranceQuotes")
@SecurityScheme(name = "insuranceBearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = "insuranceBearer")
public class GovernedQuoteJourneyController {
    private final GovernedQuoteJourneyService journey;

    public GovernedQuoteJourneyController(GovernedQuoteJourneyService journey) {
        this.journey = journey;
    }

    @PostMapping
    @DisableIdempotency
    @Operation(operationId = "offerInsuranceQuote", summary = "Request an insurance quote offer through its domain")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuoteCommandResponse.class)))
    public Mono<QuoteCommandResponse> offer(@PathVariable UUID tenantId, @PathVariable UUID submissionId,
            @PathVariable UUID partyId, @PathVariable UUID productId, @Valid @RequestBody QuoteOfferRequest request) {
        return journey.offer(new GovernedQuoteScope(tenantId, submissionId, partyId, productId), request);
    }

    @PostMapping("/{offerId}/acceptances")
    @DisableIdempotency
    @Operation(operationId = "acceptInsuranceQuote", summary = "Accept the exact existing quote terms")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuoteCommandResponse.class)))
    public Mono<QuoteCommandResponse> accept(@PathVariable UUID tenantId, @PathVariable UUID submissionId,
            @PathVariable UUID partyId, @PathVariable UUID productId, @PathVariable UUID offerId,
            @Valid @RequestBody QuoteAcceptanceRequest request) {
        return journey.accept(new GovernedQuoteScope(tenantId, submissionId, partyId, productId), offerId, request);
    }

    @GetMapping("/{offerId}")
    @DisableIdempotency
    @Operation(operationId = "readInsuranceQuoteOffer", summary = "Read the currently authorized quote offer")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuoteReadResponse.class)))
    public Mono<QuoteReadResponse> current(@PathVariable UUID tenantId, @PathVariable UUID submissionId,
            @PathVariable UUID partyId, @PathVariable UUID productId, @PathVariable UUID offerId) {
        return journey.current(new GovernedQuoteScope(tenantId, submissionId, partyId, productId), offerId);
    }

    @GetMapping
    @DisableIdempotency
    @Operation(operationId = "readInsuranceQuoteHistory", summary = "Read authorized quote history")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuoteHistoryResponse.class)))
    public Mono<QuoteHistoryResponse> history(@PathVariable UUID tenantId, @PathVariable UUID submissionId,
            @PathVariable UUID partyId, @PathVariable UUID productId) {
        return journey.history(new GovernedQuoteScope(tenantId, submissionId, partyId, productId));
    }

    @GetMapping("/actions/{actionId}/recovery")
    @DisableIdempotency
    @Operation(operationId = "recoverInsuranceQuoteAction", summary = "Observe an existing quote action without repeating its mutation")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuoteRecoveryResponse.class)))
    public Mono<QuoteRecoveryResponse> recover(@PathVariable UUID tenantId, @PathVariable UUID submissionId,
            @PathVariable UUID partyId, @PathVariable UUID productId, @PathVariable UUID actionId,
            @RequestParam String expectedIntentDigest) {
        return journey.recover(new GovernedQuoteScope(tenantId, submissionId, partyId, productId), actionId, expectedIntentDigest);
    }
}
