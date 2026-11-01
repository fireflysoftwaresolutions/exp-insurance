# Insurance binding decision boundary

Policy binding starts at `POST /api/v1/experience/insurance/policies`. The experience requires an explicit `partyId` and preserves identity/access, catalog and decision checks. Binding is currently unavailable: even APPROVE returns503 `INSURANCE_BINDING_UNAVAILABLE` before policy creation or document issuance. A referral or missing decision cannot silently become a policy command.

This guide describes the existing binding path and its guarded decision boundary. A complete insurance implementation also needs authoritative underwriting, accepted quote and coverage terms, premium/tax calculation, current customer authority, policy-core persistence, governed documents and the relevant payment/claim integrations.

## Request identity

`PolicyCommand` carries separate tenant, contract, product and party UUIDs, policy number, line of business, status, inception/expiry dates and currency. A contract UUID never substitutes for a missing party UUID. The genuine producer marks `PolicyCommand.partyId` required. The journey service checks it before identity/access, bureau, decision, policy or document calls. A missing body is handled by the HTTP layer; direct service callers also receive the explicit bad-request result for a null command.

Obtain each identifier from its owning service and retain its actual associations. The required-party check establishes presence only. It does not establish that a customer exists, owns the contract, has consented, or is authorized to bind this offer.

## Execution order

1. Validate the explicit party identity.
2. Run the existing identity/access and journey-catalog checks.
3. Obtain the credit-observation collaborator's result and request the decision.
4. Inspect the actual decision.
5. After APPROVE, return503 `INSURANCE_BINDING_UNAVAILABLE` before policy creation and document issuance.

The native `@DisableIdempotency` annotation opts this route out of transport response reuse so a prior response cannot skip current checks. The optional `X-Idempotency-Key` header remains accepted and documented for request-shape continuity; the route ignores it and derives no authority or durable business action from it. This retains the local typed candidate signature only; it does not preserve the published five-call SDK API.

The policy and document collaborators are never invoked by this binding route while the availability fence is in place. Empty credit observations and empty decision publishers are explicit unavailable outcomes. They cannot complete the reactive chain as an empty successful response. A null decision outcome also fails closed. Exceptions from collaborators retain their existing propagation path.

| Result | HTTP status | Policy/document effect |
| --- | --- | --- |
| Missing party | 400 / `INSURANCE_PARTY_REQUIRED` | Neither collaborator invoked |
| REFER | 409 / `INSURANCE_REFERRAL_REQUIRED` | Neither collaborator invoked |
| DECLINE | 422 / existing declined-decision contract | Neither collaborator invoked |
| Empty observation, empty decision or absent outcome | 503 / `INSURANCE_DECISION_UNAVAILABLE` | Neither collaborator invoked |
| APPROVE | 503 / `INSURANCE_BINDING_UNAVAILABLE` | Neither collaborator invoked |

The new referral/unavailable messages are fixed strings; they do not include the collaborator's private diagnostic reason. Existing decline semantics are preserved. Quote and claim paths retain their previous decision behavior and must be reviewed as separate journeys. In particular, this change does not endorse using a generic credit gate for first-notice-of-loss.

## Referral and retry handling

A REFER result requires an authorized underwriting/referral resolution. This service does not yet create a durable underwriting case, record an override or resume a bound offer. A channel must not relabel REFER as APPROVE or repeatedly submit it as a normal bind.

For unavailable observations/decisions, retain correlation and business identifiers and inspect the owning decision service. This guard creates no durable business receipt. The availability fence prevents new policy/document effects from this route. It creates no durable bind receipt and does not resolve earlier failed attempts. Reconcile any prior policy/contract/document effect with its actual owner before another attempt; a blind retry can repeat an existing legacy effect. Preserve actual IDs and owner observations. Do not delete a BOUND policy or manufacture a receipt as compensation.

## Adapter and deployment boundaries

The existing selectors are `firefly.experience.insurance.catalog`, `firefly.experience.credit-report.adapter`, `firefly.experience.decision.adapter` and `firefly.experience.access.adapter`. Inspect the adapter configuration classes and `exp-insurance-web/src/main/resources/application.yaml` for their declared values and endpoint configuration. The catalog and credit-report adapters currently default to sandbox; decision/access domain configurations default to domain. A sandbox result is synthetic and does not establish real underwriting or bureau acceptance.

The legacy binding path still supplies `IdentityPort.SAMPLE_USERNAME`, and its general web security configuration permits requests. Replace this legacy caller boundary with verified current identity, tenant/resource policy and per-call downstream delegation before accepting a deployed customer binding journey.

The former bind continuation created a policy, then called the deliberately closed kit-document route with the returned policy ID as contract ID. That route now returns409 migration-required. The availability fence removes that continuation. A policy UUID cannot substitute for an actual contract UUID.

Positive binding requires immutable accepted quote/version evidence, a stable owner bind action with atomic policy/history/audit/outbox and recovery, verified tenant/party/product/contract associations, and exact governed document intent. The generated governed contract API supports durable contract document actions, but its qualified `CONTRACT_DOCUMENT_V1` profile is distinct from policy wording or schedules. Replacing the client without those owner contracts cannot establish policy issuance.

## Verification

The exact focused source passed retained normal unselected verification: 229 tests, comprising 187 corrected-starter, 34 insurance-core and eight insurance-web tests, with zero failures, errors or skips. The genuine producer started, generated the 15-operation, 12-path contract and stopped; ordinary SDK generation, compilation and packaging completed. Seven separate actual generated-client HTTP checks used the candidate controller/service with explicit synthetic collaborators and recorded zero policy/document effects. These seven checks are separate from the 229-test total.

Qualification used the exact independently reviewed corrected-starter source in a private six-project reactor with pinned cached dependencies. No shared installation, released consumer cutover, native financial owner persistence or positive binding is established. The earlier larger 236-test packet is separate and does not qualify this focused tree. Independent read-only review accepted this exact local source/artifact packet; source publication does not distribute the corrected framework dependency or release the breaking SDK contract.

Run the repository's full Maven verification with the JDK required by its parent. Its producer lifecycle generates OpenAPI from the actual running OpenAPI application and then generates the SDK from that input. Do not edit the copied YAML to invent required fields or response codes.

For a focused local check without regenerating the producer:

```sh
mvn -B -DskipOpenApiGen -pl exp-insurance-web -am \
  -Dtest=InsuranceBindDecisionHttpTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

`InsuranceJourneyServiceTest` and `InsuranceBindRiskStepTest` check lazy/no-effect behavior with synthetic ports. `InsuranceBindDecisionHttpTest` starts actual loopback HTTP, the controller, service and native error handling; its policy, decision, identity and document collaborators are explicit fixtures. It checks APPROVE refusal before effects, preserved decision failures, and the actual runtime OpenAPI required-party declaration. Repeated service subscription remains effect-free. These are decision-boundary tests, not live policy-core, provider, premium, signing or end-to-end insurance acceptance.

Ordinary repeated same-body/same-header HTTP requests observe current APPROVE, REFER and absent-observation outcomes with repeated access checks and no policy/document effects. Runtime OpenAPI verifies the native opt-out marker and optional compatibility header. That ordinary HTTP fixture disables transport caching and does not qualify cache internals or replace current customer authorization.

## Generated-client migration

The published `exp-insurance-sdk:26.10.6` has five bodyless operations in `QuotesApi`, `PoliciesApi` and `ClaimsApi`. Genuine producer output exposes the existing 15 controller operations across 12 paths through `InsuranceControllerApi`, with eight typed models. The old three API classes are removed. Source compilation and binary linkage therefore require caller migration and a coordinated future version bump; do not replace the same published coordinates.

| Published call | Typed candidate call |
| --- | --- |
| `QuotesApi.createQuote()` | `InsuranceControllerApi.createQuote(QuoteCommand, String)` |
| `PoliciesApi.bindPolicy()` | `InsuranceControllerApi.bind(PolicyCommand, String)` |
| `PoliciesApi.cancelPolicy(UUID)` | `InsuranceControllerApi.cancelPolicy(UUID, ReasonCommand, String)` |
| `ClaimsApi.registerClaim()` | `InsuranceControllerApi.registerClaim(ClaimCommand, String)` |
| `ClaimsApi.closeClaim(UUID)` | `InsuranceControllerApi.closeClaim(UUID, ReasonCommand, String)` |

The final string is the optional idempotency header declared by each operation; bind ignores it through native transport opt-out. Migrate all `WithHttpInfo` and `WithResponseSpec` variants as well. Request bodies are required by these controller routes. Binding also requires an explicit party ID. Obtain real owner identifiers and associations rather than manufacturing them. Generated `InsuranceRecord` responses replace the old SDK's `Void` results; no positive bind result is currently available.

The wire paths/methods of the five older operations remain, but their bodyless published requests cannot satisfy the typed body contract. Ten additional servicing/read/status operations are now represented faithfully. Header continuity within the candidate lineage is not source, binary or complete wire compatibility with the published SDK. Inspect required fields, enums, response statuses and serialization for each migrated caller. No handwritten generated-code shim is provided. Publication requires a separately approved version and consumer migration plan.
