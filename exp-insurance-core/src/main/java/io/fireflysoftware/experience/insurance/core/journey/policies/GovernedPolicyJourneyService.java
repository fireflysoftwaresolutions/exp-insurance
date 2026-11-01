package io.fireflysoftware.experience.insurance.core.journey.policies;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface GovernedPolicyJourneyService {
    default Mono<GovernedPolicyPreparedIntent> prepare(GovernedPolicyIntentRequest request){return Mono.error(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Trusted governed policy preparation required"));}
    Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request);
    Mono<GovernedPolicyCommandResponse> issue(UUID policyId,GovernedPolicyIssueRequest request);
    Mono<GovernedPolicyRecoveryResponse> recover(UUID actionId,String intentDigest);
}
