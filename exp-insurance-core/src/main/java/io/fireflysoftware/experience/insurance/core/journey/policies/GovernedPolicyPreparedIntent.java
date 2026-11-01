package io.fireflysoftware.experience.insurance.core.journey.policies;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.UUID;

/** The Domain owner's opaque digest is an intent correlation value, never authorization or a reservation. */
@Schema(requiredProperties={"operation","actionId","intentDigest"})
public record GovernedPolicyPreparedIntent(Operation operation,UUID actionId,String intentDigest) {
    public enum Operation {BIND,ISSUE}
    public GovernedPolicyPreparedIntent {Objects.requireNonNull(operation);GovernedPolicyValues.id(actionId);GovernedPolicyValues.digest(intentDigest);}
    @Override public String toString(){return "GovernedPolicyPreparedIntent["+operation+"]";}
}
