package io.fireflysoftware.experience.insurance.core.journey.policies;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(requiredProperties={"actionId","expectedBindingVersion","bindingReceiptDigest"})
public record GovernedPolicyIssueRequest(UUID actionId,long expectedBindingVersion,String bindingReceiptDigest) {
    public GovernedPolicyIssueRequest {GovernedPolicyValues.id(actionId);GovernedPolicyValues.digest(bindingReceiptDigest);if(expectedBindingVersion<1)throw new IllegalArgumentException("Existing policy binding version required");}
    @Override public String toString(){return "GovernedPolicyIssueRequest[redacted]";}
}
