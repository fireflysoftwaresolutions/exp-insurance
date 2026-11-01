package io.fireflysoftware.experience.insurance.core.journey.policies;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.UUID;

/** Read-only original selectors. Preparation reserves nothing and cannot be used as authority. */
@Schema(requiredProperties={"operation"})
public record GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation operation,GovernedPolicyBindRequest bindRequest,
        UUID policyId,GovernedPolicyIssueRequest issueRequest) {
    public GovernedPolicyIntentRequest {
        Objects.requireNonNull(operation);
        if(operation==GovernedPolicyPreparedIntent.Operation.BIND){if(bindRequest==null || policyId!=null || issueRequest!=null)throw invalid();}
        else {if(bindRequest!=null || policyId==null || issueRequest==null)throw invalid();GovernedPolicyValues.id(policyId);}
    }
    public UUID actionId(){return operation==GovernedPolicyPreparedIntent.Operation.BIND?bindRequest.actionId():issueRequest.actionId();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("One exact original governed policy preparation selector required");}
    @Override public String toString(){return "GovernedPolicyIntentRequest["+operation+"]";}
}
