package io.fireflysoftware.experience.insurance.core.journey.policies;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(requiredProperties={"state","actionId","intentDigest"})
public record GovernedPolicyRecoveryResponse(State state,UUID actionId,String intentDigest,GovernedPolicyBindingResult result) {
    public enum State {FOUND,NOT_OBSERVED_AT_THIS_READ,INTENT_CONFLICT}
    public GovernedPolicyRecoveryResponse {Objects.requireNonNull(state);GovernedPolicyValues.id(actionId);GovernedPolicyValues.digest(intentDigest);if((state==State.FOUND)!=(result!=null))throw new IllegalArgumentException("Exact governed policy recovery observation required");}
    @Override public String toString(){return "GovernedPolicyRecoveryResponse["+state+"]";}
}
