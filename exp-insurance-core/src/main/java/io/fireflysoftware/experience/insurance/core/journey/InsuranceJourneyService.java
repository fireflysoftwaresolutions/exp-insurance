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

import io.fireflysoftware.experience.insurance.core.ports.AccessAction;
import io.fireflysoftware.experience.insurance.core.ports.AccessGuard;
import io.fireflysoftware.experience.insurance.core.ports.AccessPort;
import io.fireflysoftware.experience.insurance.core.ports.AccessRequest;
import io.fireflysoftware.experience.insurance.core.ports.CreditReportPort;
import io.fireflysoftware.experience.insurance.core.ports.CreditReportQuery;
import io.fireflysoftware.experience.insurance.core.ports.DecisionPort;
import io.fireflysoftware.experience.insurance.core.ports.DecisionRequest;
import io.fireflysoftware.experience.insurance.core.ports.IdentityPort;
import io.fireflysoftware.experience.insurance.core.ports.JourneyStep;
import io.fireflysoftware.experience.insurance.core.ports.RiskStep;
import lombok.RequiredArgsConstructor;
import org.fireflyframework.web.error.exceptions.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Customer insurance journey: quote, bind, and claim, then the servicing sagas the
 * domains already have (status, withdraw, cancel, close). Endorse and renew are not
 * here because those sagas are not on the domains. Reinsurance, reserving, and
 * insurance financials are back office.
 * <p>
 * Order: identity, access, catalog, decision only for quote, bind, and claim registration,
 * then the domain port. A denial or a decline does not call the domain.
 */
@Service
@RequiredArgsConstructor
public class InsuranceJourneyService {

    private final IdentityPort identityPort;
    private final AccessPort accessPort;
    private final CreditReportPort creditReportPort;
    private final DecisionPort decisionPort;
    private final JourneyCatalogPort catalog;
    private final UnderwritingPort underwriting;
    private final PolicyPort policies;
    private final ClaimPort claims;

    public Mono<InsuranceRecord> createQuote(QuoteCommand command) {
        return decided(InsuranceOperation.QUOTE_CREATE, "POLICY", command.partyId(), command.productId(),
                JourneyStep.INSURANCE_QUOTE,
                settings -> underwriting.create(command, settings.defaultSubmissionStatus()));
    }

    public Mono<InsuranceRecord> readQuote(UUID submissionId) {
        return plain(InsuranceOperation.QUOTE_READ, AccessAction.READ, "POLICY", submissionId,
                settings -> underwriting.get(submissionId));
    }

    public Mono<InsuranceRecord> updateQuote(UUID submissionId, QuoteCommand command) {
        return plain(InsuranceOperation.QUOTE_UPDATE, AccessAction.UPDATE, "POLICY", submissionId,
                settings -> underwriting.update(submissionId, command));
    }

    public Mono<InsuranceRecord> quoteStatus(UUID submissionId, SubmissionStatus status, String reason) {
        return plain(InsuranceOperation.QUOTE_STATUS, AccessAction.UPDATE, "POLICY", submissionId,
                settings -> underwriting.status(submissionId, status, reason));
    }

    public Mono<InsuranceRecord> withdrawQuote(UUID submissionId, String reason) {
        return plain(InsuranceOperation.QUOTE_WITHDRAW, AccessAction.CLOSE, "POLICY", submissionId,
                settings -> underwriting.withdraw(submissionId, reason));
    }

    public Mono<InsuranceRecord> bind(PolicyCommand command) {
        UUID partyId = command.partyId() != null ? command.partyId() : command.contractId();
        return decided(InsuranceOperation.POLICY_CREATE, "POLICY", partyId, command.productId(),
                JourneyStep.INSURANCE_BIND,
                settings -> policies.create(command, settings.defaultPolicyStatus()));
    }

    public Mono<InsuranceRecord> readPolicy(UUID policyId) {
        return plain(InsuranceOperation.POLICY_READ, AccessAction.READ, "POLICY", policyId,
                settings -> policies.get(policyId));
    }

    public Mono<InsuranceRecord> updatePolicy(UUID policyId, PolicyCommand command) {
        return plain(InsuranceOperation.POLICY_UPDATE, AccessAction.UPDATE, "POLICY", policyId,
                settings -> policies.update(policyId, command));
    }

    public Mono<InsuranceRecord> policyStatus(UUID policyId, PolicyStatus status, String reason) {
        return plain(InsuranceOperation.POLICY_STATUS, AccessAction.UPDATE, "POLICY", policyId,
                settings -> policies.status(policyId, status, reason));
    }

    public Mono<InsuranceRecord> cancelPolicy(UUID policyId, String reason) {
        return plain(InsuranceOperation.POLICY_CANCEL, AccessAction.CLOSE, "POLICY", policyId,
                settings -> policies.cancel(policyId, reason));
    }

    public Mono<InsuranceRecord> registerClaim(ClaimCommand command) {
        return decided(InsuranceOperation.CLAIM_CREATE, "CLAIM", command.policyId(), null,
                JourneyStep.CLAIM,
                settings -> claims.create(command, settings.defaultClaimStatus()));
    }

    public Mono<InsuranceRecord> readClaim(UUID claimId) {
        return plain(InsuranceOperation.CLAIM_READ, AccessAction.READ, "CLAIM", claimId,
                settings -> claims.get(claimId));
    }

    public Mono<InsuranceRecord> updateClaim(UUID claimId, ClaimCommand command) {
        return plain(InsuranceOperation.CLAIM_UPDATE, AccessAction.UPDATE, "CLAIM", claimId,
                settings -> claims.update(claimId, command));
    }

    public Mono<InsuranceRecord> claimStatus(UUID claimId, ClaimStatus status, String reason) {
        return plain(InsuranceOperation.CLAIM_STATUS, AccessAction.UPDATE, "CLAIM", claimId,
                settings -> claims.status(claimId, status, reason));
    }

    public Mono<InsuranceRecord> closeClaim(UUID claimId, String reason) {
        return plain(InsuranceOperation.CLAIM_CLOSE, AccessAction.CLOSE, "CLAIM", claimId,
                settings -> claims.close(claimId, reason));
    }

    private Mono<InsuranceRecord> decided(InsuranceOperation operation,
                                          String resourceType,
                                          UUID partyId,
                                          UUID productId,
                                          JourneyStep step,
                                          Function<JourneySettings, Mono<InsuranceRecord>> domain) {
        return AccessGuard.require(identityPort, accessPort, IdentityPort.SAMPLE_USERNAME,
                AccessRequest.of(partyId, AccessAction.CREATE, resourceType, null),
                () -> catalog.current(productId != null ? productId : partyId).flatMap(settings -> {
                    if (!settings.enabled(operation)) {
                        return disabled(operation);
                    }
                    return RiskStep.afterAccess(
                            creditReportPort,
                            decisionPort,
                            new CreditReportQuery(partyId, productId, step, Map.of()),
                            new DecisionRequest(partyId, productId, step, Map.of()),
                            () -> domain.apply(settings));
                }));
    }

    private Mono<InsuranceRecord> plain(InsuranceOperation operation,
                                        AccessAction action,
                                        String resourceType,
                                        UUID resourceId,
                                        Function<JourneySettings, Mono<InsuranceRecord>> domain) {
        return AccessGuard.require(identityPort, accessPort, IdentityPort.SAMPLE_USERNAME,
                AccessRequest.of(resourceId, action, resourceType, resourceId),
                () -> catalog.current(resourceId).flatMap(settings -> {
                    if (!settings.enabled(operation)) {
                        return disabled(operation);
                    }
                    return domain.apply(settings);
                }));
    }

    private static Mono<InsuranceRecord> disabled(InsuranceOperation operation) {
        return Mono.error(new BusinessException(HttpStatus.CONFLICT, "JOURNEY_NOT_ENABLED",
                operation.name() + " is not enabled"));
    }
}
