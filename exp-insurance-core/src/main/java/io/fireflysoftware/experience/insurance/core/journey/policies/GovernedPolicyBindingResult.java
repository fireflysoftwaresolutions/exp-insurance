package io.fireflysoftware.experience.insurance.core.journey.policies;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.UUID;

/** Local sandbox receipt only; IN_FORCE does not certify carrier, documents or premium/accounting effects. */
@Schema(requiredProperties={"policyId","policyNumber","policyStatus","bindingVersion","bindingReceiptDigest","acceptanceActionId","acceptanceId","offerId","offerVersion","termsDigest"})
public record GovernedPolicyBindingResult(UUID policyId,String policyNumber,PolicyState policyStatus,long bindingVersion,
        String bindingReceiptDigest,UUID acceptanceActionId,UUID acceptanceId,UUID offerId,long offerVersion,String termsDigest) {
    public enum PolicyState {BOUND,IN_FORCE}
    public GovernedPolicyBindingResult {
        GovernedPolicyValues.id(policyId);GovernedPolicyValues.text(policyNumber,64);Objects.requireNonNull(policyStatus);GovernedPolicyValues.digest(bindingReceiptDigest);
        GovernedPolicyValues.id(acceptanceActionId);GovernedPolicyValues.id(acceptanceId);GovernedPolicyValues.id(offerId);GovernedPolicyValues.digest(termsDigest);
        if(bindingVersion<1 || offerVersion<1)throw new IllegalArgumentException("Exact governed policy receipt required");
    }
    @Override public String toString(){return "GovernedPolicyBindingResult[redacted]";}
}
