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


package io.fireflysoftware.experience.insurance.core.ports;

import org.fireflyframework.web.error.exceptions.BusinessException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Order for a step that needs a bureau report and a risk decision:
 * the caller has already checked {@link AccessPort}; this runs the external data port,
 * then {@link DecisionPort}, then the domain command. A decline does not call the domain.
 */
public final class RiskStep {

    private RiskStep() {
    }

    public static <T> Mono<T> afterAccess(CreditReportPort creditReportPort,
                                          DecisionPort decisionPort,
                                          CreditReportQuery creditQuery,
                                          DecisionRequest decisionRequest,
                                          Supplier<Mono<T>> domainCall) {
        Mono<CreditReportView> reports = creditReportPort.request(creditQuery);
        if (decisionRequest.journeyStep() == JourneyStep.INSURANCE_BIND) {
            reports = reports.switchIfEmpty(Mono.error(decisionUnavailable()));
        }
        return reports.flatMap(report -> {
            // Do not copy the score. The decision service expects a number and fills 640
            // from its own sandbox bureau when the caller omits creditScore.
            DecisionRequest enriched = decisionRequest
                    .withAttribute("creditProvider", report.provider())
                    .withAttribute("creditIllustrative", Boolean.toString(report.illustrative()));
            return decisionThenDomain(decisionPort, enriched, domainCall);
        });
    }

    public static <T> Mono<T> decisionThenDomain(DecisionPort decisionPort,
                                                 DecisionRequest decisionRequest,
                                                 Supplier<Mono<T>> domainCall) {
        Mono<DecisionResult> decision = decisionPort.decide(decisionRequest);
        if (decisionRequest.journeyStep() == JourneyStep.INSURANCE_BIND) {
            decision = decision.switchIfEmpty(Mono.error(decisionUnavailable()));
        }
        return decision.flatMap(result -> {
            if (result.outcome() == DecisionOutcome.DECLINE) {
                return Mono.error(new DecisionDeclinedException(result.reasonCodes()));
            }
            if (decisionRequest.journeyStep() == JourneyStep.INSURANCE_BIND
                    && result.outcome() != DecisionOutcome.APPROVE) {
                return Mono.error(result.outcome() == DecisionOutcome.REFER
                        ? new BusinessException(HttpStatus.CONFLICT, "INSURANCE_REFERRAL_REQUIRED",
                                "Insurance binding requires an authorized referral resolution")
                        : decisionUnavailable());
            }
            return Mono.defer(domainCall);
        });
    }

    private static BusinessException decisionUnavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "INSURANCE_DECISION_UNAVAILABLE",
                "Insurance binding requires an explicit decision");
    }
}
