<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(max-width: 640px) and (prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-dark.svg">
  <source media="(max-width: 640px)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Experience - Insurance" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Experience - Insurance

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

> Customer journey for quoting cover, binding a policy, registering a claim, and servicing the policy or claim with the sagas those domains already have

## Overview

`exp-insurance` is one experience service. It is not a copy of each insurance domain. Quote uses `domain-insurance-underwriting`. Bind, status, and cancel use `domain-insurance-policies`. Registering and closing a claim use `domain-insurance-claims`.

Endorse and renew are not exposed. Those domains do not have an endorse saga or a renew saga. Reinsurance, reserving, and insurance financials are back office and are not part of this journey. Staff administration is a separate terminal.

The journey depends on ports. The current adapters call the domain HTTP sagas. A future process engine can replace an adapter without changing the journey. This service does not import a core SDK, a domain workflow class, Keycloak, or a bureau client. Jafly stays behind the security-center identity adapter and the decision service.

## Call order

1. `IdentityPort` posts to `POST /api/v1/identity/subjects` on domain-core-security-center. The sample username is `sample-journey` (`11111111-1111-4111-8111-111111111111`). `denied-actor` is `22222222-2222-4222-8222-222222222222`. Experience code does not call Keycloak.
2. `AccessPort` posts to `POST /api/v1/access/checks`. `ACTOR_NOT_ENTITLED` stops the journey. The sample customer may READ and CREATE. The customer is not granted ADMINISTER.
3. Quote, bind, and claim registration read the sandbox credit report and post `POST /api/v1/decisions` on domain-lending-credit-scoring. The journey steps are `INSURANCE_QUOTE`, `INSURANCE_BIND`, and `CLAIM`. A `DECLINE` does not call the domain. Status, withdraw, cancel, and close do not ask for a decision.
4. The domain port runs the saga: create, update, status, withdraw (quote), cancel (policy), or close (claim).

Which of those operations is enabled, and the default status sent when the caller omits one, comes from `insurance-journey-catalog.json`. `SandboxJourneyCatalogAdapter` is the class an installer replaces.

## API

| Method | Path | Saga |
|--------|------|------|
| POST | `/api/v1/experience/insurance/quotes` | Create submission |
| GET | `/api/v1/experience/insurance/quotes/{id}` | Read submission |
| PUT | `/api/v1/experience/insurance/quotes/{id}` | Update submission |
| PUT | `/api/v1/experience/insurance/quotes/{id}/status` | Status saga |
| POST | `/api/v1/experience/insurance/quotes/{id}/withdraw` | Withdraw submission |
| POST | `/api/v1/experience/insurance/policies` | Bind (create policy) |
| GET | `/api/v1/experience/insurance/policies/{id}` | Read policy |
| PUT | `/api/v1/experience/insurance/policies/{id}` | Update policy |
| PUT | `/api/v1/experience/insurance/policies/{id}/status` | Status saga |
| POST | `/api/v1/experience/insurance/policies/{id}/cancel` | Cancel policy |
| POST | `/api/v1/experience/insurance/claims` | Register claim |
| GET | `/api/v1/experience/insurance/claims/{id}` | Read claim |
| PUT | `/api/v1/experience/insurance/claims/{id}` | Update claim |
| PUT | `/api/v1/experience/insurance/claims/{id}/status` | Status saga |
| POST | `/api/v1/experience/insurance/claims/{id}/close` | Close claim |

Server port `8108`.

## Testing

```bash
mvn test
```
