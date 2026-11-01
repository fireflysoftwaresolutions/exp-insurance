package io.fireflysoftware.experience.insurance.core.journey.policies;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** Only the Domain policy service is callable here; an adapter must use its genuine generated SDK. */
public interface GovernedPolicyDomainPort {
    enum Action {BIND,ISSUE,RECOVER}
    Mono<CallerBound> current(Action action);
    interface CallerBound {
        Mono<Void> revalidate();
        Mono<GovernedPolicyPreparedIntent> prepareBind(GovernedPolicyBindRequest request);
        Mono<GovernedPolicyPreparedIntent> prepareIssue(UUID policyId,GovernedPolicyIssueRequest request);
        Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request);
        Mono<GovernedPolicyCommandResponse> issue(UUID policyId,GovernedPolicyIssueRequest request);
        Mono<GovernedPolicyRecoveryResponse> recover(UUID actionId,String intentDigest);
    }
}
