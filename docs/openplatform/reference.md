# Insurance Customer Journeys: source reference

[Repository guide](../../README.md) · [Machine-readable inventory](source-inventory.json)

This reference is derived from checked-in source. It separates declared HTTP mappings, typed contracts, property placeholders, and validation locations. Java reflection, inherited framework routes, generated targets, and runtime infrastructure are outside this static inventory.

## HTTP operations

| Mapping | Method | Controller | Kind |
| --- | --- | --- | --- |
| `GET /api/v1/experience/insurance/quotes/{submissionId}` | `public Mono<InsuranceRecord> readQuote(@PathVariable UUID submissionId)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `GET /api/v1/experience/insurance/policies/{policyId}` | `public Mono<InsuranceRecord> readPolicy(@PathVariable UUID policyId)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `GET /api/v1/experience/insurance/claims/{claimId}` | `public Mono<InsuranceRecord> readClaim(@PathVariable UUID claimId)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `POST /api/v1/experience/insurance/quotes` | `public Mono<InsuranceRecord> createQuote(@RequestBody QuoteCommand command)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `POST /api/v1/experience/insurance/quotes/{submissionId}/withdraw` | `public Mono<InsuranceRecord> withdrawQuote(@PathVariable UUID submissionId, @RequestBody ReasonCommand command)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `POST /api/v1/experience/insurance/policies` | `public Mono<InsuranceRecord> bind(@RequestBody PolicyCommand command, @Parameter(description = "Compatibility header; this route does not cache responses or derive authority from it.") @RequestHeader(name = "X-Idempotency-Key", required = false) String ignoredIdempotencyKey)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `POST /api/v1/experience/insurance/policies/{policyId}/cancel` | `public Mono<InsuranceRecord> cancelPolicy(@PathVariable UUID policyId, @RequestBody ReasonCommand command)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `POST /api/v1/experience/insurance/claims` | `public Mono<InsuranceRecord> registerClaim(@RequestBody ClaimCommand command)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `POST /api/v1/experience/insurance/claims/{claimId}/close` | `public Mono<InsuranceRecord> closeClaim(@PathVariable UUID claimId, @RequestBody ReasonCommand command)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `PUT /api/v1/experience/insurance/quotes/{submissionId}` | `public Mono<InsuranceRecord> updateQuote(@PathVariable UUID submissionId, @RequestBody QuoteCommand command)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `PUT /api/v1/experience/insurance/quotes/{submissionId}/status` | `public Mono<InsuranceRecord> quoteStatus(@PathVariable UUID submissionId, @RequestBody QuoteStatusBody body)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `PUT /api/v1/experience/insurance/policies/{policyId}` | `public Mono<InsuranceRecord> updatePolicy(@PathVariable UUID policyId, @RequestBody PolicyCommand command)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `PUT /api/v1/experience/insurance/policies/{policyId}/status` | `public Mono<InsuranceRecord> policyStatus(@PathVariable UUID policyId, @RequestBody PolicyStatusBody body)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `PUT /api/v1/experience/insurance/claims/{claimId}` | `public Mono<InsuranceRecord> updateClaim(@PathVariable UUID claimId, @RequestBody ClaimCommand command)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |
| `PUT /api/v1/experience/insurance/claims/{claimId}/status` | `public Mono<InsuranceRecord> claimStatus(@PathVariable UUID claimId, @RequestBody ClaimStatusBody body)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) | Local declaration |

## Data contracts

### ClaimCommand

[ClaimCommand](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/ClaimCommand.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `tenantId` | `UUID` | See declaration / service rules |
| `policyId` | `UUID` | See declaration / service rules |
| `claimNumber` | `String` | See declaration / service rules |
| `claimStatus` | `ClaimStatus` | See declaration / service rules |
| `lossDate` | `LocalDate` | See declaration / service rules |
| `notifiedAt` | `Instant` | See declaration / service rules |
| `causeCode` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `currency` | `String` | See declaration / service rules |

### InsuranceRecord

[InsuranceRecord](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/InsuranceRecord.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `id` | `UUID` | See declaration / service rules |
| `reference` | `String` | See declaration / service rules |
| `status` | `String` | See declaration / service rules |
| `documentId` | `UUID` | See declaration / service rules |

### JourneySettings

[JourneySettings](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/JourneySettings.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `enabled` | `EnumSet<InsuranceOperation>` | See declaration / service rules |
| `defaultSubmissionStatus` | `SubmissionStatus` | See declaration / service rules |
| `defaultPolicyStatus` | `PolicyStatus` | See declaration / service rules |
| `defaultClaimStatus` | `ClaimStatus` | See declaration / service rules |

### PolicyCommand

[PolicyCommand](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/PolicyCommand.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `tenantId` | `UUID` | See declaration / service rules |
| `contractId` | `UUID` | See declaration / service rules |
| `productId` | `UUID` | See declaration / service rules |
| `partyId` | `UUID` | See declaration / service rules |
| `policyNumber` | `String` | See declaration / service rules |
| `lineOfBusiness` | `LineOfBusiness` | See declaration / service rules |
| `policyStatus` | `PolicyStatus` | See declaration / service rules |
| `inceptionDate` | `LocalDate` | See declaration / service rules |
| `expiryDate` | `LocalDate` | See declaration / service rules |
| `currency` | `String` | See declaration / service rules |

### QuoteCommand

[QuoteCommand](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/QuoteCommand.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `tenantId` | `UUID` | See declaration / service rules |
| `partyId` | `UUID` | See declaration / service rules |
| `productId` | `UUID` | See declaration / service rules |
| `submissionNumber` | `String` | See declaration / service rules |
| `lineOfBusiness` | `LineOfBusiness` | See declaration / service rules |
| `submissionStatus` | `SubmissionStatus` | See declaration / service rules |
| `currency` | `String` | See declaration / service rules |

### ReasonCommand

[ReasonCommand](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/ReasonCommand.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `reason` | `String` | See declaration / service rules |

### AccessDecision

[AccessDecision](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/AccessDecision.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `allowed` | `boolean` | See declaration / service rules |
| `reason` | `String` | See declaration / service rules |

### AccessRequest

[AccessRequest](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/AccessRequest.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `actorId` | `UUID` | See declaration / service rules |
| `action` | `AccessAction` | See declaration / service rules |
| `resourceType` | `String` | See declaration / service rules |
| `resourceId` | `UUID` | See declaration / service rules |

### CreditReportQuery

[CreditReportQuery](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/CreditReportQuery.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `partyId` | `UUID` | See declaration / service rules |
| `productId` | `UUID` | See declaration / service rules |
| `journeyStep` | `JourneyStep` | See declaration / service rules |
| `attributes` | `String>` | See declaration / service rules |

### CreditReportView

[CreditReportView](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/CreditReportView.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `provider` | `String` | See declaration / service rules |
| `reportId` | `String` | See declaration / service rules |
| `score` | `Integer` | See declaration / service rules |
| `status` | `String` | See declaration / service rules |
| `illustrative` | `boolean` | See declaration / service rules |

### DecisionRequest

[DecisionRequest](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/DecisionRequest.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `partyId` | `UUID` | See declaration / service rules |
| `productId` | `UUID` | See declaration / service rules |
| `journeyStep` | `JourneyStep` | See declaration / service rules |
| `attributes` | `String>` | See declaration / service rules |

### DecisionResult

[DecisionResult](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/DecisionResult.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `outcome` | `DecisionOutcome` | See declaration / service rules |
| `reasonCodes` | `List<String>` | See declaration / service rules |

### SubjectRef

[SubjectRef](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/SubjectRef.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `actorId` | `UUID` | See declaration / service rules |
| `provider` | `String` | See declaration / service rules |

## Ports and service contracts

### ClaimPort

[ClaimPort](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/ClaimPort.java)

```java
Mono<InsuranceRecord> create(ClaimCommand command, ClaimStatus defaultStatus);
Mono<InsuranceRecord> get(UUID claimId);
Mono<InsuranceRecord> update(UUID claimId, ClaimCommand command);
Mono<InsuranceRecord> status(UUID claimId, ClaimStatus status, String reason);
Mono<InsuranceRecord> close(UUID claimId, String reason);
```

### JourneyCatalogPort

[JourneyCatalogPort](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/JourneyCatalogPort.java)

```java
Mono<JourneySettings> current(UUID productId);
```

### PolicyPort

[PolicyPort](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/PolicyPort.java)

```java
Mono<InsuranceRecord> create(PolicyCommand command, PolicyStatus defaultStatus);
Mono<InsuranceRecord> get(UUID policyId);
Mono<InsuranceRecord> update(UUID policyId, PolicyCommand command);
Mono<InsuranceRecord> status(UUID policyId, PolicyStatus status, String reason);
Mono<InsuranceRecord> cancel(UUID policyId, String reason);
```

### UnderwritingPort

[UnderwritingPort](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/UnderwritingPort.java)

```java
Mono<InsuranceRecord> create(QuoteCommand command, SubmissionStatus defaultStatus);
Mono<InsuranceRecord> get(UUID submissionId);
Mono<InsuranceRecord> update(UUID submissionId, QuoteCommand command);
Mono<InsuranceRecord> status(UUID submissionId, SubmissionStatus status, String reason);
Mono<InsuranceRecord> withdraw(UUID submissionId, String reason);
```

### AccessPort

[AccessPort](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/AccessPort.java)

```java
Mono<AccessDecision> check(AccessRequest request);
```

### CreditBureauClient

[CreditBureauClient](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/CreditBureauClient.java)

```java
Mono<CreditReportView> request(CreditReportQuery query);
```

### CreditReportPort

[CreditReportPort](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/CreditReportPort.java)

```java
Mono<CreditReportView> request(CreditReportQuery query);
```

### DecisionPort

[DecisionPort](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/DecisionPort.java)

```java
Mono<DecisionResult> decide(DecisionRequest request);
```

### IdentityPort

[IdentityPort](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/IdentityPort.java)

```java
Mono<SubjectRef> resolve(String username);
```

### RiskDecisionClient

[RiskDecisionClient](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/RiskDecisionClient.java)

```java
Mono<DecisionResult> decide(DecisionRequest request);
```

### SecurityCenterClient

[SecurityCenterClient](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/SecurityCenterClient.java)

```java
Mono<AccessDecision> check(AccessRequest request);
```

## Configuration

Values below come from source placeholders. Plain YAML values and configuration-property classes must also be reviewed at the linked configuration sources. Secret fallback values are deliberately redacted.

| Key | Fallback | Source |
| --- | --- | --- |
| `api-configuration.domain-platform.security-center.base-path` | `http://localhost:8085` | [DomainAccessConfiguration](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/adapter/DomainAccessConfiguration.java) |
| `api-configuration.domain-platform.lending-credit-scoring.base-path` | `http://localhost:8042` | [DomainDecisionConfiguration](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/adapter/DomainDecisionConfiguration.java) |
| `api-configuration.domain-platform.insurance-underwriting.base-path` | `http://localhost:8073` | [HttpInsuranceConfiguration](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/adapter/HttpInsuranceConfiguration.java) |
| `api-configuration.domain-platform.insurance-policies.base-path` | `http://localhost:8071` | [HttpInsuranceConfiguration](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/adapter/HttpInsuranceConfiguration.java) |
| `api-configuration.domain-platform.insurance-claims.base-path` | `http://localhost:8072` | [HttpInsuranceConfiguration](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/adapter/HttpInsuranceConfiguration.java) |
| `firefly.insurance.contracts-base-url` | `http://localhost:8080` | [KitDocumentClientConfiguration](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/KitDocumentClientConfiguration.java) |
| `SERVER_PORT` | `8108` | [application](../../exp-insurance-web/src/main/resources/application.yaml) |
| `ENDPOINT_DOMAIN_CORE_SECURITY_CENTER` | `http://localhost:8085` | [application](../../exp-insurance-web/src/main/resources/application.yaml) |
| `ENDPOINT_DOMAIN_LENDING_CREDIT_SCORING` | `http://localhost:8042` | [application](../../exp-insurance-web/src/main/resources/application.yaml) |
| `ENDPOINT_DOMAIN_INSURANCE_UNDERWRITING` | `http://localhost:8073` | [application](../../exp-insurance-web/src/main/resources/application.yaml) |
| `ENDPOINT_DOMAIN_INSURANCE_POLICIES` | `http://localhost:8071` | [application](../../exp-insurance-web/src/main/resources/application.yaml) |
| `ENDPOINT_DOMAIN_INSURANCE_CLAIMS` | `http://localhost:8072` | [application](../../exp-insurance-web/src/main/resources/application.yaml) |

## Security and failure evidence

| Declaration or failure site | Source |
| --- | --- |
| `new IllegalArgumentException("partyId and journeyStep are required"));` | [DomainDecisionConfiguration](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/adapter/DomainDecisionConfiguration.java) |
| `new IllegalStateException("DOMAIN_RESPONSE_UNREADABLE");` | [DomainDecisionConfiguration](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/adapter/DomainDecisionConfiguration.java) |
| `new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", name + " is required");` | [HttpInsuranceConfiguration](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/adapter/HttpInsuranceConfiguration.java) |
| `new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", name + " is required");` | [HttpInsuranceConfiguration](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/adapter/HttpInsuranceConfiguration.java) |
| `new UncheckedIOException(ex);` | [SandboxJourneyCatalogAdapter](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/adapter/SandboxJourneyCatalogAdapter.java) |
| `new BusinessException(HttpStatus.BAD_REQUEST, "INSURANCE_PARTY_REQUIRED",` | [InsuranceJourneyService](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/InsuranceJourneyService.java) |
| `new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,` | [InsuranceJourneyService](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/InsuranceJourneyService.java) |
| `new BusinessException(HttpStatus.CONFLICT, "JOURNEY_NOT_ENABLED",` | [InsuranceJourneyService](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/journey/InsuranceJourneyService.java) |
| `new AccessDeniedException(decision.reason())));` | [AccessGuard](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/AccessGuard.java) |
| `new AccessDeniedException(decision.reason())));` | [AccessGuard](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/AccessGuard.java) |
| `new AccessDeniedException("ACTOR_UNKNOWN")))` | [AccessGuard](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/AccessGuard.java) |
| `HttpStatus.UNPROCESSABLE_ENTITY` | [DecisionDeclinedException](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/DecisionDeclinedException.java) |
| `new DecisionDeclinedException(result.reasonCodes()));` | [RiskStep](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/RiskStep.java) |
| `new BusinessException(HttpStatus.CONFLICT, "INSURANCE_REFERRAL_REQUIRED",` | [RiskStep](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/RiskStep.java) |
| `new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "INSURANCE_DECISION_UNAVAILABLE",` | [RiskStep](../../exp-insurance-core/src/main/java/io/fireflysoftware/experience/insurance/core/ports/RiskStep.java) |
| `@ResponseStatus(HttpStatus.CREATED)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) |
| `@ResponseStatus(HttpStatus.CREATED)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) |
| `@ResponseStatus(HttpStatus.CREATED)` | [InsuranceController](../../exp-insurance-web/src/main/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceController.java) |

## Build and dependencies

### exp-insurance-core

[exp-insurance-core/pom.xml](../../exp-insurance-core/pom.xml)

Coordinates: `io.fireflysoftware:exp-insurance-core:26.10.6`. Parent: `io.fireflysoftware:exp-insurance:26.10.6`.

- `io.fireflysoftware:domain-common-contracts-sdk:26.10.6`
- `io.fireflysoftware:domain-core-security-center-sdk:26.10.6`
- `io.fireflysoftware:domain-insurance-claims-sdk:26.10.6`
- `io.fireflysoftware:domain-insurance-policies-sdk:26.10.6`
- `io.fireflysoftware:domain-insurance-underwriting-sdk:26.10.6`
- `io.fireflysoftware:domain-lending-credit-scoring-sdk:26.10.6`
- `io.projectreactor:reactor-test:`
- `org.fireflyframework:fireflyframework-web:`
- `org.projectlombok:lombok:`
- `org.springframework.boot:spring-boot-starter-test:`
- `org.springframework.boot:spring-boot-starter-webflux:`

### exp-insurance-sdk

[exp-insurance-sdk/pom.xml](../../exp-insurance-sdk/pom.xml)

Coordinates: `io.fireflysoftware:exp-insurance-sdk:26.10.6`. Parent: `io.fireflysoftware:exp-insurance:26.10.6`.

- `com.fasterxml.jackson.core:jackson-databind:`
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310:`
- `com.squareup.okhttp3:logging-interceptor:`
- `com.squareup.okhttp3:okhttp:`
- `io.swagger.core.v3:swagger-annotations:`
- `jakarta.annotation:jakarta.annotation-api:`
- `javax.annotation:javax.annotation-api:1.3.2`
- `org.openapitools:jackson-databind-nullable:0.2.8`
- `org.springframework:spring-webflux:`

### exp-insurance-web

[exp-insurance-web/pom.xml](../../exp-insurance-web/pom.xml)

Coordinates: `io.fireflysoftware:exp-insurance-web:26.10.6`. Parent: `io.fireflysoftware:exp-insurance:26.10.6`.

- `io.fireflysoftware:exp-insurance-core:`
- `io.projectreactor:reactor-test:`
- `org.fireflyframework:fireflyframework-starter-application:`
- `org.fireflyframework:fireflyframework-web:`
- `org.projectlombok:lombok:`
- `org.springdoc:springdoc-openapi-starter-webflux-ui:`
- `org.springframework.boot:spring-boot-starter-actuator:`
- `org.springframework.boot:spring-boot-starter-test:`
- `org.springframework.boot:spring-boot-starter-validation:`
- `org.springframework.boot:spring-boot-starter-webflux-test:`
- `org.springframework.boot:spring-boot-starter-webflux:`

### exp-insurance

[pom.xml](../../pom.xml)

Coordinates: `io.fireflysoftware:exp-insurance:26.10.6`. Parent: `io.fireflysoftware:firefly-parent:26.10.6`.

- `io.fireflysoftware:exp-insurance-core:${project.version}`
- `io.fireflysoftware:exp-insurance-sdk:${project.version}`
- `io.fireflysoftware:exp-insurance-web:${project.version}`

## Test sources

- [exp-insurance-core/src/test/java/io/fireflysoftware/experience/insurance/core/adapter/HttpPolicyClientTest.java](../../exp-insurance-core/src/test/java/io/fireflysoftware/experience/insurance/core/adapter/HttpPolicyClientTest.java)
- [exp-insurance-core/src/test/java/io/fireflysoftware/experience/insurance/core/adapter/HttpUnderwritingClientTest.java](../../exp-insurance-core/src/test/java/io/fireflysoftware/experience/insurance/core/adapter/HttpUnderwritingClientTest.java)
- [exp-insurance-core/src/test/java/io/fireflysoftware/experience/insurance/core/journey/InsuranceJourneyServiceTest.java](../../exp-insurance-core/src/test/java/io/fireflysoftware/experience/insurance/core/journey/InsuranceJourneyServiceTest.java)
- [exp-insurance-core/src/test/java/io/fireflysoftware/experience/insurance/core/ports/InsuranceBindRiskStepTest.java](../../exp-insurance-core/src/test/java/io/fireflysoftware/experience/insurance/core/ports/InsuranceBindRiskStepTest.java)
- [exp-insurance-web/src/test/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceBindDecisionHttpTest.java](../../exp-insurance-web/src/test/java/io/fireflysoftware/experience/insurance/web/controllers/InsuranceBindDecisionHttpTest.java)
- [exp-insurance-web/src/test/java/io/fireflysoftware/experience/insurance/web/openapi/OpenApiGenApplication.java](../../exp-insurance-web/src/test/java/io/fireflysoftware/experience/insurance/web/openapi/OpenApiGenApplication.java)

## CI definitions

- [.github/workflows/release.yml](../../.github/workflows/release.yml)
