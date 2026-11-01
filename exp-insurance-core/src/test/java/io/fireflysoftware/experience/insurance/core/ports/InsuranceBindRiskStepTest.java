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
package io.fireflysoftware.experience.insurance.core.ports;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.fireflyframework.web.error.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class InsuranceBindRiskStepTest {
    private DecisionRequest request(JourneyStep step) {
        return new DecisionRequest(UUID.randomUUID(), UUID.randomUUID(), step, Map.of());
    }

    @Test
    void directBindReferralNeverEvaluatesTheDomainSupplier() {
        var calls = new AtomicInteger();
        StepVerifier.create(RiskStep.decisionThenDomain(
                request -> Mono.just(new DecisionResult(DecisionOutcome.REFER, List.of("private-marker"))),
                request(JourneyStep.INSURANCE_BIND),
                () -> { calls.incrementAndGet(); return Mono.just("bound"); }))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).getStatus().value()).isEqualTo(409);
                    assertThat(error.getMessage()).doesNotContain("private-marker");
                }).verify();
        assertThat(calls.get()).isZero();
    }

    @Test
    void approvedBindInvokesSupplierOnlyAfterSubscription() {
        var calls = new AtomicInteger();
        var work = RiskStep.decisionThenDomain(request -> Mono.just(DecisionResult.approve()),
                request(JourneyStep.INSURANCE_BIND),
                () -> { calls.incrementAndGet(); return Mono.just("bound"); });
        assertThat(calls.get()).isZero();
        StepVerifier.create(work).expectNext("bound").verifyComplete();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void directBindMissingDecisionFailsUnavailable() {
        StepVerifier.create(RiskStep.decisionThenDomain(request -> Mono.empty(),
                request(JourneyStep.INSURANCE_BIND), () -> Mono.error(new AssertionError("supplier"))))
                .expectErrorMatches(error -> error instanceof BusinessException business
                        && business.getStatus().value() == 503).verify();
    }

    @Test
    void directBindUnknownOutcomeFailsUnavailable() {
        StepVerifier.create(RiskStep.decisionThenDomain(request -> Mono.just(new DecisionResult(null, List.of())),
                request(JourneyStep.INSURANCE_BIND), () -> Mono.error(new AssertionError("supplier"))))
                .expectErrorMatches(error -> error instanceof BusinessException business
                        && business.getStatus().value() == 503).verify();
    }

    @Test
    void directBindDeclineKeepsExistingDeclineResult() {
        StepVerifier.create(RiskStep.decisionThenDomain(request -> Mono.just(new DecisionResult(DecisionOutcome.DECLINE, List.of("DECLINED"))),
                request(JourneyStep.INSURANCE_BIND), () -> Mono.error(new AssertionError("supplier"))))
                .expectError(DecisionDeclinedException.class).verify();
    }

    @Test
    void bindEmptyCreditObservationDoesNotReturnEmptySuccess() {
        var decisionCalls = new AtomicInteger();
        DecisionPort decision = request -> { decisionCalls.incrementAndGet(); return Mono.just(DecisionResult.approve()); };
        StepVerifier.create(RiskStep.afterAccess(query -> Mono.empty(), decision,
                new CreditReportQuery(UUID.randomUUID(), UUID.randomUUID(), JourneyStep.INSURANCE_BIND, Map.of()),
                request(JourneyStep.INSURANCE_BIND), () -> Mono.error(new AssertionError("supplier"))))
                .expectErrorMatches(error -> error instanceof BusinessException business
                        && business.getStatus().value() == 503).verify();
        assertThat(decisionCalls.get()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = JourneyStep.class, names = {"INSURANCE_QUOTE", "CLAIM"})
    void preservesOtherOperationsExistingReferralBehavior(JourneyStep step) {
        StepVerifier.create(RiskStep.decisionThenDomain(
                request -> Mono.just(new DecisionResult(DecisionOutcome.REFER, List.of())),
                request(step), () -> Mono.just("existing-intake")))
                .expectNext("existing-intake").verifyComplete();
    }
}
