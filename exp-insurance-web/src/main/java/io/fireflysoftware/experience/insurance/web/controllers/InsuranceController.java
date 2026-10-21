/*
 * Copyright 2025 Firefly Software Foundation
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

import io.fireflysoftware.experience.insurance.core.journey.ClaimCommand;
import io.fireflysoftware.experience.insurance.core.journey.ClaimStatus;
import io.fireflysoftware.experience.insurance.core.journey.InsuranceJourneyService;
import io.fireflysoftware.experience.insurance.core.journey.InsuranceRecord;
import io.fireflysoftware.experience.insurance.core.journey.PolicyCommand;
import io.fireflysoftware.experience.insurance.core.journey.PolicyStatus;
import io.fireflysoftware.experience.insurance.core.journey.QuoteCommand;
import io.fireflysoftware.experience.insurance.core.journey.ReasonCommand;
import io.fireflysoftware.experience.insurance.core.journey.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Customer insurance journey. Staff administration is a different service.
 */
@RestController
@RequestMapping("/api/v1/experience/insurance")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceJourneyService journey;

    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<InsuranceRecord> createQuote(@RequestBody QuoteCommand command) {
        return journey.createQuote(command);
    }

    @GetMapping("/quotes/{submissionId}")
    public Mono<InsuranceRecord> readQuote(@PathVariable UUID submissionId) {
        return journey.readQuote(submissionId);
    }

    @PutMapping("/quotes/{submissionId}")
    public Mono<InsuranceRecord> updateQuote(@PathVariable UUID submissionId, @RequestBody QuoteCommand command) {
        return journey.updateQuote(submissionId, command);
    }

    @PutMapping("/quotes/{submissionId}/status")
    public Mono<InsuranceRecord> quoteStatus(@PathVariable UUID submissionId,
                                             @RequestBody QuoteStatusBody body) {
        return journey.quoteStatus(submissionId, body.status(), body.reason());
    }

    @PostMapping("/quotes/{submissionId}/withdraw")
    public Mono<InsuranceRecord> withdrawQuote(@PathVariable UUID submissionId, @RequestBody ReasonCommand command) {
        return journey.withdrawQuote(submissionId, command.reason());
    }

    @PostMapping("/policies")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<InsuranceRecord> bind(@RequestBody PolicyCommand command) {
        return journey.bind(command);
    }

    @GetMapping("/policies/{policyId}")
    public Mono<InsuranceRecord> readPolicy(@PathVariable UUID policyId) {
        return journey.readPolicy(policyId);
    }

    @PutMapping("/policies/{policyId}")
    public Mono<InsuranceRecord> updatePolicy(@PathVariable UUID policyId, @RequestBody PolicyCommand command) {
        return journey.updatePolicy(policyId, command);
    }

    @PutMapping("/policies/{policyId}/status")
    public Mono<InsuranceRecord> policyStatus(@PathVariable UUID policyId, @RequestBody PolicyStatusBody body) {
        return journey.policyStatus(policyId, body.status(), body.reason());
    }

    @PostMapping("/policies/{policyId}/cancel")
    public Mono<InsuranceRecord> cancelPolicy(@PathVariable UUID policyId, @RequestBody ReasonCommand command) {
        return journey.cancelPolicy(policyId, command.reason());
    }

    @PostMapping("/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<InsuranceRecord> registerClaim(@RequestBody ClaimCommand command) {
        return journey.registerClaim(command);
    }

    @GetMapping("/claims/{claimId}")
    public Mono<InsuranceRecord> readClaim(@PathVariable UUID claimId) {
        return journey.readClaim(claimId);
    }

    @PutMapping("/claims/{claimId}")
    public Mono<InsuranceRecord> updateClaim(@PathVariable UUID claimId, @RequestBody ClaimCommand command) {
        return journey.updateClaim(claimId, command);
    }

    @PutMapping("/claims/{claimId}/status")
    public Mono<InsuranceRecord> claimStatus(@PathVariable UUID claimId, @RequestBody ClaimStatusBody body) {
        return journey.claimStatus(claimId, body.status(), body.reason());
    }

    @PostMapping("/claims/{claimId}/close")
    public Mono<InsuranceRecord> closeClaim(@PathVariable UUID claimId, @RequestBody ReasonCommand command) {
        return journey.closeClaim(claimId, command.reason());
    }

    public record QuoteStatusBody(SubmissionStatus status, String reason) {
    }

    public record PolicyStatusBody(PolicyStatus status, String reason) {
    }

    public record ClaimStatusBody(ClaimStatus status, String reason) {
    }
}
