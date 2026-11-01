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

import io.fireflysoftware.domain.insurance.claims.sdk.api.ClaimsApi;
import io.fireflysoftware.domain.insurance.claims.sdk.model.ClaimDTO;
import io.fireflysoftware.domain.insurance.policies.sdk.api.PoliciesApi;
import io.fireflysoftware.domain.insurance.policies.sdk.model.PolicyDTO;
import io.fireflysoftware.domain.insurance.policies.sdk.model.PolicyCreateRequest;
import io.fireflysoftware.domain.insurance.underwriting.sdk.api.UnderwritingApi;
import io.fireflysoftware.domain.insurance.underwriting.sdk.model.SubmissionDTO;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Quote, bind, and claim calls go through the insurance domain SDKs.
 */
@Configuration
public class HttpInsuranceConfiguration {

    @Bean
    public UnderwritingPort underwritingPort(
            @Value("${api-configuration.domain-platform.insurance-underwriting.base-path:http://localhost:8073}")
            String basePath) {
        io.fireflysoftware.domain.insurance.underwriting.sdk.invoker.ApiClient client =
                new io.fireflysoftware.domain.insurance.underwriting.sdk.invoker.ApiClient();
        client.setBasePath(basePath);
        return new HttpUnderwritingClient(new UnderwritingApi(client));
    }

    @Bean
    public PolicyPort policyPort(
            @Value("${api-configuration.domain-platform.insurance-policies.base-path:http://localhost:8071}")
            String basePath) {
        io.fireflysoftware.domain.insurance.policies.sdk.invoker.ApiClient client =
                new io.fireflysoftware.domain.insurance.policies.sdk.invoker.ApiClient();
        client.setBasePath(basePath);
        return new HttpPolicyClient(new PoliciesApi(client));
    }

    @Bean
    public ClaimPort claimPort(
            @Value("${api-configuration.domain-platform.insurance-claims.base-path:http://localhost:8072}")
            String basePath) {
        io.fireflysoftware.domain.insurance.claims.sdk.invoker.ApiClient client =
                new io.fireflysoftware.domain.insurance.claims.sdk.invoker.ApiClient();
        client.setBasePath(basePath);
        return new HttpClaimClient(new ClaimsApi(client));
    }

    public static final class HttpUnderwritingClient implements UnderwritingPort {

        private final UnderwritingApi underwriting;

        public HttpUnderwritingClient(UnderwritingApi underwriting) {
            this.underwriting = underwriting;
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
            SubmissionDTO body = new SubmissionDTO()
                    .tenantId(command.tenantId())
                    .partyId(command.partyId())
                    .productId(command.productId())
                    .submissionNumber(command.submissionNumber())
                    .lineOfBusiness(SubmissionDTO.LineOfBusinessEnum.fromValue(command.lineOfBusiness().name()))
                    .submissionStatus(SubmissionDTO.SubmissionStatusEnum.fromValue(status.name()))
                    .receivedAt(LocalDateTime.now(ZoneOffset.UTC));
            if (command.currency() != null) {
                body.currency(command.currency());
            }
            return underwriting.create(body, key()).map(HttpInsuranceConfiguration::submission);
        }

        @Override
        public Mono<InsuranceRecord> get(UUID submissionId) {
            return underwriting.get(submissionId, key()).map(HttpInsuranceConfiguration::submission);
        }

        @Override
        public Mono<InsuranceRecord> update(UUID submissionId, QuoteCommand command) {
            require(command.lineOfBusiness(), "lineOfBusiness");
            SubmissionDTO body = new SubmissionDTO()
                    .tenantId(command.tenantId())
                    .partyId(command.partyId())
                    .productId(command.productId())
                    .submissionNumber(command.submissionNumber())
                    .lineOfBusiness(SubmissionDTO.LineOfBusinessEnum.fromValue(command.lineOfBusiness().name()))
                    .receivedAt(LocalDateTime.now(ZoneOffset.UTC));
            if (command.submissionStatus() != null) {
                body.submissionStatus(SubmissionDTO.SubmissionStatusEnum.fromValue(command.submissionStatus().name()));
            }
            if (command.currency() != null) {
                body.currency(command.currency());
            }
            return underwriting.update(submissionId, body, key()).map(HttpInsuranceConfiguration::submission);
        }

        @Override
        public Mono<InsuranceRecord> status(UUID submissionId, SubmissionStatus status, String reason) {
            require(status, "submissionStatus");
            io.fireflysoftware.domain.insurance.underwriting.sdk.model.StatusChangeRequest request =
                    new io.fireflysoftware.domain.insurance.underwriting.sdk.model.StatusChangeRequest()
                            .status(status.name())
                            .statusReason(reason);
            return underwriting.updateStatus(submissionId, request, key()).map(HttpInsuranceConfiguration::submission);
        }

        @Override
        public Mono<InsuranceRecord> withdraw(UUID submissionId, String reason) {
            io.fireflysoftware.domain.insurance.underwriting.sdk.model.ReasonRequest request =
                    new io.fireflysoftware.domain.insurance.underwriting.sdk.model.ReasonRequest().statusReason(reason);
            return underwriting.withdraw(submissionId, request, key()).map(HttpInsuranceConfiguration::submission);
        }
    }

    public static final class HttpPolicyClient implements PolicyPort {

        private final PoliciesApi policies;

        public HttpPolicyClient(PoliciesApi policies) {
            this.policies = policies;
        }

        @Override
        public Mono<InsuranceRecord> create(PolicyCommand command, PolicyStatus defaultStatus) {
            require(command.tenantId(), "tenantId");
            require(command.contractId(), "contractId");
            require(command.partyId(), "partyId");
            require(command.productId(), "productId");
            requireText(command.policyNumber(), "policyNumber");
            require(command.lineOfBusiness(), "lineOfBusiness");
            require(command.inceptionDate(), "inceptionDate");
            PolicyStatus status = command.policyStatus() != null ? command.policyStatus() : defaultStatus;
            require(status, "policyStatus");
            PolicyCreateRequest body = new PolicyCreateRequest()
                    .partyId(command.partyId())
                    .tenantId(command.tenantId())
                    .contractId(command.contractId())
                    .productId(command.productId())
                    .policyNumber(command.policyNumber())
                    .lineOfBusiness(PolicyCreateRequest.LineOfBusinessEnum.fromValue(command.lineOfBusiness().name()))
                    .policyStatus(PolicyCreateRequest.PolicyStatusEnum.fromValue(status.name()))
                    .inceptionDate(command.inceptionDate())
                    .expiryDate(command.expiryDate())
                    .currency(command.currency());
            return policies.create(body, key()).map(HttpInsuranceConfiguration::policy);
        }

        @Override
        public Mono<InsuranceRecord> get(UUID policyId) {
            return policies.get(policyId, key()).map(HttpInsuranceConfiguration::policy);
        }

        @Override
        public Mono<InsuranceRecord> update(UUID policyId, PolicyCommand command) {
            require(command.lineOfBusiness(), "lineOfBusiness");
            return policies.update(policyId, policy(command, command.policyStatus()), key())
                    .map(HttpInsuranceConfiguration::policy);
        }

        @Override
        public Mono<InsuranceRecord> status(UUID policyId, PolicyStatus status, String reason) {
            require(status, "policyStatus");
            io.fireflysoftware.domain.insurance.policies.sdk.model.StatusChangeRequest request =
                    new io.fireflysoftware.domain.insurance.policies.sdk.model.StatusChangeRequest()
                            .status(status.name())
                            .statusReason(reason);
            return policies.updateStatus(policyId, request, key()).map(HttpInsuranceConfiguration::policy);
        }

        @Override
        public Mono<InsuranceRecord> cancel(UUID policyId, String reason) {
            io.fireflysoftware.domain.insurance.policies.sdk.model.ReasonRequest request =
                    new io.fireflysoftware.domain.insurance.policies.sdk.model.ReasonRequest().statusReason(reason);
            return policies.cancel(policyId, request, key()).map(HttpInsuranceConfiguration::policy);
        }

        private static PolicyDTO policy(PolicyCommand command, PolicyStatus status) {
            PolicyDTO body = new PolicyDTO()
                    .tenantId(command.tenantId())
                    .contractId(command.contractId())
                    .productId(command.productId())
                    .policyNumber(command.policyNumber());
            if (command.lineOfBusiness() != null) {
                body.lineOfBusiness(PolicyDTO.LineOfBusinessEnum.fromValue(command.lineOfBusiness().name()));
            }
            if (status != null) {
                body.policyStatus(PolicyDTO.PolicyStatusEnum.fromValue(status.name()));
            }
            if (command.inceptionDate() != null) {
                body.inceptionDate(command.inceptionDate());
            }
            if (command.expiryDate() != null) {
                body.expiryDate(command.expiryDate());
            }
            if (command.currency() != null) {
                body.currency(command.currency());
            }
            return body;
        }
    }

    public static final class HttpClaimClient implements ClaimPort {

        private final ClaimsApi claims;

        public HttpClaimClient(ClaimsApi claims) {
            this.claims = claims;
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
            return claims.create(claim(command, status), key()).map(HttpInsuranceConfiguration::claim);
        }

        @Override
        public Mono<InsuranceRecord> get(UUID claimId) {
            return claims.get(claimId, key()).map(HttpInsuranceConfiguration::claim);
        }

        @Override
        public Mono<InsuranceRecord> update(UUID claimId, ClaimCommand command) {
            return claims.update(claimId, claim(command, command.claimStatus()), key()).map(HttpInsuranceConfiguration::claim);
        }

        @Override
        public Mono<InsuranceRecord> status(UUID claimId, ClaimStatus status, String reason) {
            require(status, "claimStatus");
            io.fireflysoftware.domain.insurance.claims.sdk.model.StatusChangeRequest request =
                    new io.fireflysoftware.domain.insurance.claims.sdk.model.StatusChangeRequest()
                            .status(status.name())
                            .statusReason(reason);
            return claims.updateStatus(claimId, request, key()).map(HttpInsuranceConfiguration::claim);
        }

        @Override
        public Mono<InsuranceRecord> close(UUID claimId, String reason) {
            io.fireflysoftware.domain.insurance.claims.sdk.model.ReasonRequest request =
                    new io.fireflysoftware.domain.insurance.claims.sdk.model.ReasonRequest().statusReason(reason);
            return claims.close(claimId, request, key()).map(HttpInsuranceConfiguration::claim);
        }

        private static ClaimDTO claim(ClaimCommand command, ClaimStatus status) {
            ClaimDTO body = new ClaimDTO()
                    .tenantId(command.tenantId())
                    .policyId(command.policyId())
                    .claimNumber(command.claimNumber());
            if (status != null) {
                body.claimStatus(ClaimDTO.ClaimStatusEnum.fromValue(status.name()));
            }
            if (command.lossDate() != null) {
                body.lossDate(command.lossDate());
            }
            if (command.notifiedAt() != null) {
                body.notifiedAt(LocalDateTime.ofInstant(command.notifiedAt(), ZoneOffset.UTC));
            }
            if (command.causeCode() != null) {
                body.causeCode(command.causeCode());
            }
            if (command.description() != null) {
                body.description(command.description());
            }
            if (command.currency() != null) {
                body.currency(command.currency());
            }
            return body;
        }
    }

    private static InsuranceRecord submission(SubmissionDTO dto) {
        return new InsuranceRecord(dto.getSubmissionId(), dto.getSubmissionNumber(),
                dto.getSubmissionStatus() == null ? null : dto.getSubmissionStatus().getValue());
    }

    private static InsuranceRecord policy(PolicyDTO dto) {
        return new InsuranceRecord(dto.getPolicyId(), dto.getPolicyNumber(),
                dto.getPolicyStatus() == null ? null : dto.getPolicyStatus().getValue());
    }

    private static InsuranceRecord claim(ClaimDTO dto) {
        return new InsuranceRecord(dto.getClaimId(), dto.getClaimNumber(),
                dto.getClaimStatus() == null ? null : dto.getClaimStatus().getValue());
    }

    private static String key() {
        return UUID.randomUUID().toString();
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
