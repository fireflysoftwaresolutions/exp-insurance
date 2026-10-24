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


package io.fireflysoftware.experience.insurance.core.journey;

import io.fireflysoftware.domain.common.contracts.sdk.api.KitDocumentsControllerApi;
import io.fireflysoftware.domain.common.contracts.sdk.model.RecordedId;
import io.fireflysoftware.experience.insurance.core.ports.AccessDecision;
import io.fireflysoftware.experience.insurance.core.ports.AccessDeniedException;
import io.fireflysoftware.experience.insurance.core.ports.AccessPort;
import io.fireflysoftware.experience.insurance.core.ports.CreditReportPort;
import io.fireflysoftware.experience.insurance.core.ports.CreditReportView;
import io.fireflysoftware.experience.insurance.core.ports.DecisionDeclinedException;
import io.fireflysoftware.experience.insurance.core.ports.DecisionOutcome;
import io.fireflysoftware.experience.insurance.core.ports.DecisionPort;
import io.fireflysoftware.experience.insurance.core.ports.DecisionResult;
import io.fireflysoftware.experience.insurance.core.ports.IdentityPort;
import io.fireflysoftware.experience.insurance.core.ports.JourneyStep;
import io.fireflysoftware.experience.insurance.core.ports.SubjectRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InsuranceJourneyServiceTest {

    @Mock private IdentityPort identityPort;
    @Mock private AccessPort accessPort;
    @Mock private CreditReportPort creditReportPort;
    @Mock private DecisionPort decisionPort;
    @Mock private JourneyCatalogPort catalog;
    @Mock private UnderwritingPort underwriting;
    @Mock private PolicyPort policies;
    @Mock private ClaimPort claims;
    @Mock private KitDocumentsControllerApi kitDocumentsApi;

    private InsuranceJourneyService service;

    private final JourneySettings enabled = new JourneySettings(
            EnumSet.allOf(InsuranceOperation.class),
            SubmissionStatus.RECEIVED,
            PolicyStatus.BOUND,
            ClaimStatus.NOTIFIED);

    @BeforeEach
    void setUp() {
        lenient().when(kitDocumentsApi.issue(any(), any())).thenReturn(Mono.just(new RecordedId().id(UUID.randomUUID())));
        service = new InsuranceJourneyService(identityPort, accessPort, creditReportPort, decisionPort,
                catalog, underwriting, policies, claims, kitDocumentsApi);
        lenient().when(identityPort.resolve(any())).thenReturn(Mono.just(new SubjectRef(IdentityPort.SAMPLE_ACTOR, "sandbox")));
        lenient().when(accessPort.check(any())).thenReturn(Mono.just(AccessDecision.permit("SANDBOX_GRANT")));
        lenient().when(catalog.current(any())).thenReturn(Mono.just(enabled));
    }

    @Test
    void createQuote_callsUnderwritingAfterDecision() {
        when(creditReportPort.request(any())).thenReturn(Mono.just(
                new CreditReportView("sandbox", "sandbox-report", 640, "ILLUSTRATIVE", true)));
        when(decisionPort.decide(any())).thenReturn(Mono.just(new DecisionResult(DecisionOutcome.APPROVE, List.of())));
        UUID id = UUID.randomUUID();
        when(underwriting.create(any(), any())).thenReturn(Mono.just(new InsuranceRecord(id, "Q-1", "RECEIVED")));

        QuoteCommand command = quote();
        StepVerifier.create(service.createQuote(command))
                .expectNextMatches(record -> id.equals(record.id()))
                .verifyComplete();

        verify(decisionPort).decide(org.mockito.ArgumentMatchers.argThat(request ->
                request.journeyStep() == JourneyStep.INSURANCE_QUOTE));
        verify(underwriting).create(command, SubmissionStatus.RECEIVED);
    }

    @Test
    void createQuote_declineDoesNotCallUnderwriting() {
        when(creditReportPort.request(any())).thenReturn(Mono.just(
                new CreditReportView("sandbox", "sandbox-report", 640, "ILLUSTRATIVE", true)));
        when(decisionPort.decide(any())).thenReturn(Mono.just(
                new DecisionResult(DecisionOutcome.DECLINE, List.of("HIGH_RISK"))));

        StepVerifier.create(service.createQuote(quote()))
                .expectError(DecisionDeclinedException.class)
                .verify();

        verify(underwriting, never()).create(any(), any());
    }

    @Test
    void createQuote_deniedActorDoesNotCallDecisionOrDomain() {
        when(accessPort.check(any())).thenReturn(Mono.just(AccessDecision.denied("ACTOR_NOT_ENTITLED")));

        StepVerifier.create(service.createQuote(quote()))
                .expectError(AccessDeniedException.class)
                .verify();

        verify(decisionPort, never()).decide(any());
        verify(creditReportPort, never()).request(any());
        verify(underwriting, never()).create(any(), any());
    }

    @Test
    void bind_approvedDecisionFailsBeforePolicyOrLegacyDocumentEffects() {
        when(creditReportPort.request(any())).thenReturn(Mono.just(
                new CreditReportView("sandbox", "sandbox-report", 640, "ILLUSTRATIVE", true)));
        when(decisionPort.decide(any())).thenReturn(Mono.just(DecisionResult.approve()));
        lenient().when(policies.create(any(), any())).thenReturn(Mono.just(
                new InsuranceRecord(UUID.randomUUID(), "P-1", "BOUND")));
        lenient().when(kitDocumentsApi.issue(any(), any())).thenReturn(Mono.error(
                new org.fireflyframework.web.error.exceptions.BusinessException(
                        org.springframework.http.HttpStatus.CONFLICT, "GOVERNED_DOCUMENT_INTENT_REQUIRED",
                        "Complete governed document intent is required")));

        StepVerifier.create(service.bind(policy()))
                .expectErrorMatches(error -> error instanceof org.fireflyframework.web.error.exceptions.BusinessException business
                        && business.getStatus().value() == 503
                        && "INSURANCE_BINDING_UNAVAILABLE".equals(business.getCode()))
                .verify();

        verify(decisionPort).decide(org.mockito.ArgumentMatchers.argThat(request ->
                request.journeyStep() == JourneyStep.INSURANCE_BIND));
        verifyNoInteractions(policies, kitDocumentsApi);
    }

    @Test
    void bind_repeatedSubscriptionCannotCreateAnotherPolicy() {
        when(creditReportPort.request(any())).thenReturn(Mono.just(
                new CreditReportView("sandbox", "sandbox-report", 640, "ILLUSTRATIVE", true)));
        when(decisionPort.decide(any())).thenReturn(Mono.just(DecisionResult.approve()));
        lenient().when(policies.create(any(), any())).thenReturn(Mono.just(
                new InsuranceRecord(UUID.randomUUID(), "P-1", "BOUND")));
        Mono<InsuranceRecord> request = service.bind(policy());

        for (int subscription = 0; subscription < 2; subscription++) {
            StepVerifier.create(request)
                    .expectErrorMatches(error -> error instanceof org.fireflyframework.web.error.exceptions.BusinessException business
                            && business.getStatus().value() == 503
                            && "INSURANCE_BINDING_UNAVAILABLE".equals(business.getCode()))
                    .verify();
        }
        verifyNoInteractions(policies, kitDocumentsApi);
    }

    @Test
    void bind_declineStillPrecedesBindingAvailability() {
        when(creditReportPort.request(any())).thenReturn(Mono.just(
                new CreditReportView("sandbox", "sandbox-report", 640, "ILLUSTRATIVE", true)));
        when(decisionPort.decide(any())).thenReturn(Mono.just(
                new DecisionResult(DecisionOutcome.DECLINE, List.of("HIGH_RISK"))));

        StepVerifier.create(service.bind(policy()))
                .expectError(DecisionDeclinedException.class)
                .verify();
        verifyNoInteractions(policies, kitDocumentsApi);
    }

    @Test
    void bind_deniedAccessStillPrecedesBindingAvailability() {
        when(accessPort.check(any())).thenReturn(Mono.just(AccessDecision.denied("ACTOR_NOT_ENTITLED")));

        StepVerifier.create(service.bind(policy()))
                .expectError(AccessDeniedException.class)
                .verify();
        verifyNoInteractions(creditReportPort, decisionPort, policies, kitDocumentsApi);
    }

    @Test
    void bind_disabledJourneyStillPrecedesBindingAvailability() {
        when(catalog.current(any())).thenReturn(Mono.just(new JourneySettings(
                EnumSet.noneOf(InsuranceOperation.class), SubmissionStatus.RECEIVED,
                PolicyStatus.BOUND, ClaimStatus.NOTIFIED)));

        StepVerifier.create(service.bind(policy()))
                .expectErrorMatches(error -> error instanceof org.fireflyframework.web.error.exceptions.BusinessException business
                        && business.getStatus().value() == 409
                        && "JOURNEY_NOT_ENABLED".equals(business.getCode()))
                .verify();
        verifyNoInteractions(creditReportPort, decisionPort, policies, kitDocumentsApi);
    }

    @Test
    void bind_referralDoesNotCreatePolicyOrDocument() {
        lenient().when(policies.create(any(), any())).thenReturn(Mono.just(new InsuranceRecord(UUID.randomUUID(), "P-1", "BOUND")));
        when(creditReportPort.request(any())).thenReturn(Mono.just(
                new CreditReportView("sandbox", "sandbox-report", 640, "ILLUSTRATIVE", true)));
        when(decisionPort.decide(any())).thenReturn(Mono.just(
                new DecisionResult(DecisionOutcome.REFER, List.of("private-provider-value"))));

        StepVerifier.create(service.bind(policy()))
                .expectErrorMatches(error -> error instanceof org.fireflyframework.web.error.exceptions.BusinessException business
                        && business.getStatus().value() == 409
                        && !error.getMessage().contains("private-provider-value"))
                .verify();
        verifyNoInteractions(policies, kitDocumentsApi);
    }

    @Test
    void bind_missingOutcomeDoesNotCreatePolicyOrDocument() {
        lenient().when(policies.create(any(), any())).thenReturn(Mono.just(new InsuranceRecord(UUID.randomUUID(), "P-1", "BOUND")));
        when(creditReportPort.request(any())).thenReturn(Mono.just(
                new CreditReportView("sandbox", "sandbox-report", 640, "ILLUSTRATIVE", true)));
        when(decisionPort.decide(any())).thenReturn(Mono.just(new DecisionResult(null, List.of())));

        StepVerifier.create(service.bind(policy()))
                .expectErrorMatches(error -> error instanceof org.fireflyframework.web.error.exceptions.BusinessException business
                        && business.getStatus().value() == 503)
                .verify();
        verifyNoInteractions(policies, kitDocumentsApi);
    }

    @Test
    void bind_emptyDecisionDoesNotReturnAnEmptySuccess() {
        when(creditReportPort.request(any())).thenReturn(Mono.just(
                new CreditReportView("sandbox", "sandbox-report", 640, "ILLUSTRATIVE", true)));
        when(decisionPort.decide(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.bind(policy()))
                .expectErrorMatches(error -> error instanceof org.fireflyframework.web.error.exceptions.BusinessException business
                        && business.getStatus().value() == 503)
                .verify();
        verifyNoInteractions(policies, kitDocumentsApi);
    }

    @Test
    void bind_missingPartyDoesNotUseContractAsCustomerIdentity() {
        lenient().when(creditReportPort.request(any())).thenReturn(Mono.just(
                new CreditReportView("sandbox", "sandbox-report", 640, "ILLUSTRATIVE", true)));
        lenient().when(decisionPort.decide(any())).thenReturn(Mono.just(DecisionResult.approve()));
        lenient().when(policies.create(any(), any())).thenReturn(Mono.just(new InsuranceRecord(UUID.randomUUID(), "P-1", "BOUND")));
        var command = policy();
        var invalid = new PolicyCommand(command.tenantId(), command.contractId(), command.productId(), null,
                command.policyNumber(), command.lineOfBusiness(), command.policyStatus(), command.inceptionDate(),
                command.expiryDate(), command.currency());

        StepVerifier.create(service.bind(invalid))
                .expectErrorMatches(error -> error instanceof org.fireflyframework.web.error.exceptions.BusinessException business
                        && business.getStatus().value() == 400)
                .verify();
        verifyNoInteractions(identityPort, accessPort, creditReportPort, decisionPort, policies, kitDocumentsApi);
    }

    @Test
    void registerClaim_declineDoesNotCallClaims() {
        when(creditReportPort.request(any())).thenReturn(Mono.just(
                new CreditReportView("sandbox", "sandbox-report", 640, "ILLUSTRATIVE", true)));
        when(decisionPort.decide(any())).thenReturn(Mono.just(
                new DecisionResult(DecisionOutcome.DECLINE, List.of("HIGH_RISK"))));

        StepVerifier.create(service.registerClaim(claim()))
                .expectError(DecisionDeclinedException.class)
                .verify();

        verify(claims, never()).create(any(), any());
        verify(decisionPort).decide(org.mockito.ArgumentMatchers.argThat(request ->
                request.journeyStep() == JourneyStep.CLAIM));
    }

    @Test
    void cancelPolicy_doesNotAskForADecision() {
        UUID policyId = UUID.randomUUID();
        when(policies.cancel(policyId, "customer request")).thenReturn(
                Mono.just(new InsuranceRecord(policyId, "P-1", "CANCELLED")));

        StepVerifier.create(service.cancelPolicy(policyId, "customer request"))
                .expectNextCount(1)
                .verifyComplete();

        verify(decisionPort, never()).decide(any());
        verify(creditReportPort, never()).request(any());
        verify(policies).cancel(policyId, "customer request");
    }

    @Test
    void closeClaim_doesNotAskForADecision() {
        UUID claimId = UUID.randomUUID();
        when(claims.close(claimId, "paid")).thenReturn(Mono.just(new InsuranceRecord(claimId, "C-1", "CLOSED")));

        StepVerifier.create(service.closeClaim(claimId, "paid"))
                .expectNextCount(1)
                .verifyComplete();

        verify(decisionPort, never()).decide(any());
    }

    private static QuoteCommand quote() {
        return new QuoteCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Q-1", LineOfBusiness.MOTOR, null, "USD");
    }

    private static PolicyCommand policy() {
        return new PolicyCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "P-1", LineOfBusiness.PROPERTY, null, LocalDate.of(2026, 1, 1), null, "USD");
    }

    private static ClaimCommand claim() {
        return new ClaimCommand(UUID.randomUUID(), UUID.randomUUID(), "C-1", null,
                LocalDate.of(2026, 2, 1), Instant.parse("2026-02-02T00:00:00Z"), null, null, "USD");
    }
}
