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


package io.fireflysoftware.experience.insurance.core.adapter;

import io.fireflysoftware.experience.insurance.core.journey.ClaimCommand;
import io.fireflysoftware.experience.insurance.core.journey.ClaimPort;
import io.fireflysoftware.experience.insurance.core.journey.ClaimStatus;
import io.fireflysoftware.experience.insurance.core.journey.InsuranceRecord;
import io.fireflysoftware.experience.insurance.core.journey.PolicyCommand;
import io.fireflysoftware.experience.insurance.core.journey.PolicyPort;
import io.fireflysoftware.experience.insurance.core.journey.PolicyStatus;
import io.fireflysoftware.experience.insurance.core.journey.QuoteCommand;
import io.fireflysoftware.experience.insurance.core.journey.SubmissionStatus;
import io.fireflysoftware.experience.insurance.core.journey.UnderwritingPort;
import org.fireflyframework.web.error.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP adapters for the insurance domain sagas. A future BPM replaces these beans.
 * This configuration does not import a core SDK, a domain workflow, or Keycloak.
 */
@Configuration
public class HttpInsuranceConfiguration {

    @Bean
    public UnderwritingPort underwritingPort(
            @Value("${api-configuration.domain-platform.insurance-underwriting.base-path:http://localhost:8073}")
            String basePath) {
        return new HttpUnderwritingClient(WebClient.builder().baseUrl(basePath).build());
    }

    @Bean
    public PolicyPort policyPort(
            @Value("${api-configuration.domain-platform.insurance-policies.base-path:http://localhost:8071}")
            String basePath) {
        return new HttpPolicyClient(WebClient.builder().baseUrl(basePath).build());
    }

    @Bean
    public ClaimPort claimPort(
            @Value("${api-configuration.domain-platform.insurance-claims.base-path:http://localhost:8072}")
            String basePath) {
        return new HttpClaimClient(WebClient.builder().baseUrl(basePath).build());
    }

    public static final class HttpUnderwritingClient implements UnderwritingPort {

        private final WebClient webClient;

        public HttpUnderwritingClient(WebClient webClient) {
            this.webClient = webClient;
        }

        @Override
        public Mono<InsuranceRecord> create(QuoteCommand command, SubmissionStatus defaultStatus) {
            require(command.tenantId(), "tenantId");
            require(command.partyId(), "partyId");
            require(command.productId(), "productId");
            requireText(command.submissionNumber(), "submissionNumber");
            require(command.lineOfBusiness(), "lineOfBusiness");
            SubmissionStatus status = command.submissionStatus() != null ? command.submissionStatus() : defaultStatus;
            require(status, "submissionStatus");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tenantId", command.tenantId());
            body.put("partyId", command.partyId());
            body.put("productId", command.productId());
            body.put("submissionNumber", command.submissionNumber());
            body.put("lineOfBusiness", command.lineOfBusiness().name());
            body.put("submissionStatus", status.name());
            body.put("receivedAt", Instant.now().toString());
            if (command.currency() != null) {
                body.put("currency", command.currency());
            }
            return post("/api/v1/submissions", body, "submissionId", "submissionNumber", "submissionStatus");
        }

        @Override
        public Mono<InsuranceRecord> get(UUID submissionId) {
            return read("/api/v1/submissions/" + submissionId, "submissionId", "submissionNumber", "submissionStatus");
        }

        @Override
        public Mono<InsuranceRecord> update(UUID submissionId, QuoteCommand command) {
            require(command.lineOfBusiness(), "lineOfBusiness");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tenantId", command.tenantId());
            body.put("partyId", command.partyId());
            body.put("productId", command.productId());
            body.put("submissionNumber", command.submissionNumber());
            body.put("lineOfBusiness", command.lineOfBusiness().name());
            if (command.submissionStatus() != null) {
                body.put("submissionStatus", command.submissionStatus().name());
            }
            if (command.currency() != null) {
                body.put("currency", command.currency());
            }
            return put("/api/v1/submissions/" + submissionId, body, "submissionId", "submissionNumber", "submissionStatus");
        }

        @Override
        public Mono<InsuranceRecord> status(UUID submissionId, SubmissionStatus status, String reason) {
            require(status, "submissionStatus");
            return put("/api/v1/submissions/" + submissionId + "/status", statusBody(status, reason),
                    "submissionId", "submissionNumber", "submissionStatus");
        }

        @Override
        public Mono<InsuranceRecord> withdraw(UUID submissionId, String reason) {
            return post("/api/v1/submissions/" + submissionId + "/withdraw", reasonBody(reason),
                    "submissionId", "submissionNumber", "submissionStatus");
        }

        private Mono<InsuranceRecord> post(String path, Map<String, Object> body, String id, String ref, String status) {
            return exchange(webClient.post().uri(path).bodyValue(body), id, ref, status);
        }

        private Mono<InsuranceRecord> put(String path, Map<String, Object> body, String id, String ref, String status) {
            return exchange(webClient.put().uri(path).bodyValue(body), id, ref, status);
        }

        private Mono<InsuranceRecord> read(String path, String id, String ref, String status) {
            return exchange(webClient.get().uri(path), id, ref, status);
        }
    }

    public static final class HttpPolicyClient implements PolicyPort {

        private final WebClient webClient;

        public HttpPolicyClient(WebClient webClient) {
            this.webClient = webClient;
        }

        @Override
        public Mono<InsuranceRecord> create(PolicyCommand command, PolicyStatus defaultStatus) {
            require(command.tenantId(), "tenantId");
            require(command.contractId(), "contractId");
            require(command.productId(), "productId");
            requireText(command.policyNumber(), "policyNumber");
            require(command.lineOfBusiness(), "lineOfBusiness");
            require(command.inceptionDate(), "inceptionDate");
            PolicyStatus status = command.policyStatus() != null ? command.policyStatus() : defaultStatus;
            require(status, "policyStatus");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tenantId", command.tenantId());
            body.put("contractId", command.contractId());
            body.put("productId", command.productId());
            body.put("policyNumber", command.policyNumber());
            body.put("lineOfBusiness", command.lineOfBusiness().name());
            body.put("policyStatus", status.name());
            body.put("inceptionDate", command.inceptionDate().toString());
            if (command.expiryDate() != null) {
                body.put("expiryDate", command.expiryDate().toString());
            }
            if (command.currency() != null) {
                body.put("currency", command.currency());
            }
            return exchange(webClient.post().uri("/api/v1/policies").bodyValue(body),
                    "policyId", "policyNumber", "policyStatus");
        }

        @Override
        public Mono<InsuranceRecord> get(UUID policyId) {
            return exchange(webClient.get().uri("/api/v1/policies/" + policyId),
                    "policyId", "policyNumber", "policyStatus");
        }

        @Override
        public Mono<InsuranceRecord> update(UUID policyId, PolicyCommand command) {
            require(command.lineOfBusiness(), "lineOfBusiness");
            Map<String, Object> body = basePolicy(command);
            return exchange(webClient.put().uri("/api/v1/policies/" + policyId).bodyValue(body),
                    "policyId", "policyNumber", "policyStatus");
        }

        @Override
        public Mono<InsuranceRecord> status(UUID policyId, PolicyStatus status, String reason) {
            require(status, "policyStatus");
            return exchange(webClient.put().uri("/api/v1/policies/" + policyId + "/status")
                            .bodyValue(statusBody(status, reason)),
                    "policyId", "policyNumber", "policyStatus");
        }

        @Override
        public Mono<InsuranceRecord> cancel(UUID policyId, String reason) {
            return exchange(webClient.post().uri("/api/v1/policies/" + policyId + "/cancel")
                            .bodyValue(reasonBody(reason)),
                    "policyId", "policyNumber", "policyStatus");
        }

        private static Map<String, Object> basePolicy(PolicyCommand command) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tenantId", command.tenantId());
            body.put("contractId", command.contractId());
            body.put("productId", command.productId());
            body.put("policyNumber", command.policyNumber());
            if (command.lineOfBusiness() != null) {
                body.put("lineOfBusiness", command.lineOfBusiness().name());
            }
            if (command.policyStatus() != null) {
                body.put("policyStatus", command.policyStatus().name());
            }
            if (command.inceptionDate() != null) {
                body.put("inceptionDate", command.inceptionDate().toString());
            }
            if (command.expiryDate() != null) {
                body.put("expiryDate", command.expiryDate().toString());
            }
            if (command.currency() != null) {
                body.put("currency", command.currency());
            }
            return body;
        }
    }

    public static final class HttpClaimClient implements ClaimPort {

        private final WebClient webClient;

        public HttpClaimClient(WebClient webClient) {
            this.webClient = webClient;
        }

        @Override
        public Mono<InsuranceRecord> create(ClaimCommand command, ClaimStatus defaultStatus) {
            require(command.tenantId(), "tenantId");
            require(command.policyId(), "policyId");
            requireText(command.claimNumber(), "claimNumber");
            require(command.lossDate(), "lossDate");
            require(command.notifiedAt(), "notifiedAt");
            ClaimStatus status = command.claimStatus() != null ? command.claimStatus() : defaultStatus;
            require(status, "claimStatus");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tenantId", command.tenantId());
            body.put("policyId", command.policyId());
            body.put("claimNumber", command.claimNumber());
            body.put("claimStatus", status.name());
            body.put("lossDate", command.lossDate().toString());
            body.put("notifiedAt", command.notifiedAt().toString());
            if (command.causeCode() != null) {
                body.put("causeCode", command.causeCode());
            }
            if (command.description() != null) {
                body.put("description", command.description());
            }
            if (command.currency() != null) {
                body.put("currency", command.currency());
            }
            return exchange(webClient.post().uri("/api/v1/claims").bodyValue(body),
                    "claimId", "claimNumber", "claimStatus");
        }

        @Override
        public Mono<InsuranceRecord> get(UUID claimId) {
            return exchange(webClient.get().uri("/api/v1/claims/" + claimId),
                    "claimId", "claimNumber", "claimStatus");
        }

        @Override
        public Mono<InsuranceRecord> update(UUID claimId, ClaimCommand command) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tenantId", command.tenantId());
            body.put("policyId", command.policyId());
            body.put("claimNumber", command.claimNumber());
            if (command.claimStatus() != null) {
                body.put("claimStatus", command.claimStatus().name());
            }
            if (command.lossDate() != null) {
                body.put("lossDate", command.lossDate().toString());
            }
            if (command.notifiedAt() != null) {
                body.put("notifiedAt", command.notifiedAt().toString());
            }
            if (command.description() != null) {
                body.put("description", command.description());
            }
            return exchange(webClient.put().uri("/api/v1/claims/" + claimId).bodyValue(body),
                    "claimId", "claimNumber", "claimStatus");
        }

        @Override
        public Mono<InsuranceRecord> status(UUID claimId, ClaimStatus status, String reason) {
            require(status, "claimStatus");
            return exchange(webClient.put().uri("/api/v1/claims/" + claimId + "/status")
                            .bodyValue(statusBody(status, reason)),
                    "claimId", "claimNumber", "claimStatus");
        }

        @Override
        public Mono<InsuranceRecord> close(UUID claimId, String reason) {
            return exchange(webClient.post().uri("/api/v1/claims/" + claimId + "/close")
                            .bodyValue(reasonBody(reason)),
                    "claimId", "claimNumber", "claimStatus");
        }
    }

    private static Map<String, Object> statusBody(Enum<?> status, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.name());
        body.put("statusReason", reason);
        return body;
    }

    private static Map<String, Object> reasonBody(String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("statusReason", reason);
        return body;
    }

    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {
            };

    private static Mono<InsuranceRecord> exchange(WebClient.RequestHeadersSpec<?> spec,
                                                  String idField, String referenceField, String statusField) {
        return spec.retrieve().bodyToMono(MAP)
                .map(node -> new InsuranceRecord(
                        uuid(node.get(idField)),
                        text(node.get(referenceField)),
                        text(node.get(statusField))));
    }

    private static UUID uuid(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return UUID.fromString(value.toString());
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static void require(Object value, String name) {
        if (value == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", name + " is required");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", name + " is required");
        }
    }

}
