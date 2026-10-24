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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;

import io.fireflysoftware.domain.common.contracts.sdk.api.KitDocumentsControllerApi;
import io.fireflysoftware.experience.insurance.core.journey.*;
import io.fireflysoftware.experience.insurance.core.ports.*;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Real controller/service/error HTTP boundary with explicit synthetic decision and owner ports. */
class InsuranceBindDecisionHttpTest {
    static final AtomicInteger ACCESS_CALLS = new AtomicInteger();
    static final AtomicInteger DECISION_CALLS = new AtomicInteger();
    static final AtomicReference<Mono<DecisionResult>> DECISION = new AtomicReference<>();
    static final AtomicReference<Mono<CreditReportView>> REPORT = new AtomicReference<>();
    static final PolicyPort POLICIES = mock(PolicyPort.class);
    static final KitDocumentsControllerApi DOCUMENTS = mock(KitDocumentsControllerApi.class);
    static ConfigurableApplicationContext context;
    static WebClient client;

    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"})
    @Import({InsuranceController.class,
            io.fireflysoftware.experience.insurance.web.config.WebSecurityConfig.class})
    static class TestApp {
        @Bean
        InsuranceJourneyService journey() {
            return new InsuranceJourneyService(
                    username -> Mono.just(new SubjectRef(IdentityPort.SAMPLE_ACTOR, "explicit-test-fixture")),
                    request -> {
                        ACCESS_CALLS.incrementAndGet();
                        return Mono.just(AccessDecision.permit("EXPLICIT_TEST_FIXTURE"));
                    },
                    request -> REPORT.get(),
                    request -> {
                        DECISION_CALLS.incrementAndGet();
                        return DECISION.get();
                    },
                    id -> Mono.just(new JourneySettings(EnumSet.allOf(InsuranceOperation.class),
                            SubmissionStatus.RECEIVED, PolicyStatus.BOUND, ClaimStatus.NOTIFIED)),
                    mock(UnderwritingPort.class), POLICIES, mock(ClaimPort.class), DOCUMENTS);
        }
    }

    @BeforeAll
    static void start() {
        context = new SpringApplicationBuilder(TestApp.class).run(
                "--spring.config.location=optional:classpath:/insurance-bind-test-empty.yaml",
                "--spring.application.name=insurance-bind-boundary",
                "--server.port=0", "--server.address=127.0.0.1",
                "--firefly.eda.enabled=false", "--firefly.web.idempotency.enabled=false",
                "--management.tracing.enabled=false", "--otel.sdk.disabled=true");
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        client = WebClient.builder().baseUrl("http://127.0.0.1:" + port).build();
    }

    @AfterAll
    static void stop() {
        if (context != null) context.close();
    }

    @BeforeEach
    void defaults() {
        reset(POLICIES, DOCUMENTS);
        ACCESS_CALLS.set(0);
        DECISION_CALLS.set(0);
        REPORT.set(Mono.just(new CreditReportView("synthetic", "synthetic-report", 640, "ILLUSTRATIVE", true)));
        DECISION.set(Mono.just(DecisionResult.approve()));
    }

    private PolicyCommand command(UUID party) {
        return new PolicyCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), party,
                "SYNTHETIC-POLICY", LineOfBusiness.PROPERTY, PolicyStatus.BOUND,
                LocalDate.of(2026, 1, 1), null, "USD");
    }

    private void denied(PolicyCommand command, int status, String code) {
        var result = client.post().uri("/api/v1/experience/insurance/policies")
                .header("X-Idempotency-Key", "synthetic-bind-retry")
                .bodyValue(command).exchangeToMono(response -> response.toEntity(String.class))
                .block(Duration.ofSeconds(10));
        assertThat(result).isNotNull();
        assertThat(result.getStatusCode().value()).isEqualTo(status);
        assertThat(result.getBody()).contains(code).doesNotContain("private-provider-marker");
        verifyNoInteractions(POLICIES, DOCUMENTS);
    }

    @Test
    void approvedDecisionReturnsUnavailableBeforePolicyOrDocumentEffects() {
        org.mockito.Mockito.lenient().when(POLICIES.create(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.just(new InsuranceRecord(UUID.randomUUID(), "SYNTHETIC-POLICY", "BOUND")));
        org.mockito.Mockito.lenient().when(DOCUMENTS.issue(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.error(new org.fireflyframework.web.error.exceptions.BusinessException(
                        org.springframework.http.HttpStatus.CONFLICT, "GOVERNED_DOCUMENT_INTENT_REQUIRED",
                        "Complete governed document intent is required")));
        denied(command(UUID.randomUUID()), 503, "INSURANCE_BINDING_UNAVAILABLE");
    }

    @Test
    void repeatedHttpRequestChecksCurrentAccessAndDecisionBeforeAnyEffect() {
        PolicyCommand command = command(UUID.randomUUID());
        denied(command, 503, "INSURANCE_BINDING_UNAVAILABLE");
        assertThat(ACCESS_CALLS.get()).isEqualTo(1);
        assertThat(DECISION_CALLS.get()).isEqualTo(1);

        DECISION.set(Mono.just(new DecisionResult(DecisionOutcome.REFER, List.of())));
        denied(command, 409, "INSURANCE_REFERRAL_REQUIRED");
        assertThat(ACCESS_CALLS.get()).isEqualTo(2);
        assertThat(DECISION_CALLS.get()).isEqualTo(2);

        REPORT.set(Mono.empty());
        denied(command, 503, "INSURANCE_DECISION_UNAVAILABLE");
        assertThat(ACCESS_CALLS.get()).isEqualTo(3);
        assertThat(DECISION_CALLS.get()).isEqualTo(2);
    }

    @Test
    void referralReturnsConflictWithoutPolicyOrDocumentEffect() {
        DECISION.set(Mono.just(new DecisionResult(DecisionOutcome.REFER, List.of("private-provider-marker"))));
        denied(command(UUID.randomUUID()), 409, "INSURANCE_REFERRAL_REQUIRED");
    }

    @Test
    void missingOutcomeReturnsUnavailableWithoutEffects() {
        DECISION.set(Mono.just(new DecisionResult(null, List.of("private-provider-marker"))));
        denied(command(UUID.randomUUID()), 503, "INSURANCE_DECISION_UNAVAILABLE");
    }

    @Test
    void emptyDecisionReturnsUnavailableWithoutEffects() {
        DECISION.set(Mono.empty());
        denied(command(UUID.randomUUID()), 503, "INSURANCE_DECISION_UNAVAILABLE");
    }

    @Test
    void emptyCreditObservationReturnsUnavailableWithoutEffects() {
        REPORT.set(Mono.empty());
        denied(command(UUID.randomUUID()), 503, "INSURANCE_DECISION_UNAVAILABLE");
    }

    @Test
    void missingPartyReturnsBadRequestWithoutEffects() {
        denied(command(null), 400, "INSURANCE_PARTY_REQUIRED");
    }

    @Test
    void actualPublicContractDocumentsTheBindDecisionResults() throws Exception {
        var json = client.get().uri("/v3/api-docs").retrieve().bodyToMono(String.class)
                .block(Duration.ofSeconds(10));
        var specification = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(json);
        var operation = specification.path("paths").path("/api/v1/experience/insurance/policies").path("post");
        var responses = operation.path("responses");
        assertThat(operation.path("x-disable-idempotency").asBoolean()).isTrue();
        var headers = operation.path("parameters");
        assertThat(headers.findValuesAsText("name")).contains("X-Idempotency-Key");
        var compatibilityHeader = java.util.stream.StreamSupport.stream(headers.spliterator(), false)
                .filter(parameter -> "X-Idempotency-Key".equals(parameter.path("name").asText()))
                .findFirst().orElseThrow();
        assertThat(compatibilityHeader.path("required").asBoolean()).isFalse();
        assertThat(compatibilityHeader.path("description").asText()).contains("Compatibility header");
        for (String code : List.of("201", "400", "409", "422", "503")) {
            assertThat(responses.has(code)).as("Bind response " + code).isTrue();
        }
        assertThat(specification.path("components").path("schemas").path("PolicyCommand").path("required").toString())
                .contains("\"partyId\"");
    }
}
