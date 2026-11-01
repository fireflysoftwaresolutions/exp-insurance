# Governed insurance quotes

The governed quote route connects the customer channel to Domain Underwriting through its generated SDK. Domain Underwriting invokes the Core Underwriting owner. Experience does not persist quote data, calculate premiums, create acceptance evidence, or call a Core SDK.

The normal hosted producer and consumer reactor passed all 94 selected cases on October 31, 2026, including the eight Experience service and caller-client cases. Core Underwriting, Domain Underwriting, Domain Policies, and Experience SDKs were generated from their original producers with the official generator; each packaged specification matched the captured producer bytes. This source-build result does not establish the real three-layer deployment, policy binding, carrier issuance, or production insurance coverage. The existing policy-binding boundary remains in place until its separate owner integration is delivered.

## Configuration

Configure these values explicitly:

| Property | Purpose |
| --- | --- |
| `insurance.governed.security.issuer` | Trusted issuer for incoming customer access tokens. |
| `insurance.governed.security.audience` | Required Experience audience. |
| `insurance.governed.security.jwk-set-uri` | Trusted signing-key endpoint. |
| `api-configuration.domain-platform.insurance-underwriting.base-path` | Domain Underwriting endpoint. |
| `insurance.governed.underwriting-audience` | Audience required before relaying the caller to Domain Underwriting. |
| `insurance.governed.allow-loopback` | Explicit local-fixture HTTP permission; defaults to `false`. |

Use HTTPS outside an explicitly configured loopback fixture. Missing trust rejects authentication. A missing Domain endpoint or audience prevents the downstream call. No sample username, shared bearer token, machine identity substitution, or business-data fallback is used.

The incoming token must be valid for Experience and for the configured Domain audience. Domain owns the separately configured delegation needed to reach Core; an Experience token is not promoted into Core authority. Every call acquires a new generated client with the current caller's bearer credential.

## Operations

The route prefix is `/api/v1/experience/insurance/tenants/{tenantId}/submissions/{submissionId}/parties/{partyId}/products/{productId}/quote-offers`.

| Method and suffix | Operation |
| --- | --- |
| `POST` | Request a quote using a stable action ID and exact product revision expectations. |
| `POST /{offerId}/acceptances` | Accept the selected offer version, control generation, and terms digest. |
| `GET /{offerId}` | Read the currently authorized offer. |
| `GET` | Read authorized owner history. |
| `GET /actions/{actionId}/recovery?expectedIntentDigest=…` | Observe an existing action without repeating its mutation. |

A write returns the owner's typed outcome: `COMMITTED`, `COMMITTED_RESPONSE_UNAVAILABLE`, `OUTCOME_UNKNOWN`, or `REFUSED`. HTTP 200 alone does not establish a committed offer or acceptance. The returned action ID must match the submitted action; transport failures and empty responses do not become fabricated refusals or successful absence. Keep the original action ID and intent when resolving an uncertain outcome.

These routes bypass generic response idempotency caching. Business replay and current authorization remain the owning Domain/Core responsibilities. The normal controller-generated OpenAPI and official SDK generator define the client contract; generated client code and specifications are not edited by hand.
