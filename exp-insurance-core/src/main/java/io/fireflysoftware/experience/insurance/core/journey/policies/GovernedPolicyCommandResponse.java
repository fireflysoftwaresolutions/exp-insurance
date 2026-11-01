package io.fireflysoftware.experience.insurance.core.journey.policies;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(requiredProperties={"status","actionId","intentDigest"})
public record GovernedPolicyCommandResponse(Status status,UUID actionId,String intentDigest,GovernedPolicyBindingResult result) {
    public enum Status {COMMITTED,COMMITTED_RESPONSE_UNAVAILABLE,OUTCOME_UNKNOWN,REFUSED}
    public GovernedPolicyCommandResponse {Objects.requireNonNull(status);GovernedPolicyValues.id(actionId);GovernedPolicyValues.digest(intentDigest);if((status==Status.COMMITTED)!=(result!=null))throw new IllegalArgumentException("Exact governed policy command outcome required");}
    @Override public String toString(){return "GovernedPolicyCommandResponse["+status+"]";}
}
