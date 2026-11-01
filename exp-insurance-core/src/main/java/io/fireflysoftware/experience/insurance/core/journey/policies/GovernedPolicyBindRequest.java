package io.fireflysoftware.experience.insurance.core.journey.policies;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Accepted-offer selectors only; no actor, tenant authority, premium or accepted terms. */
@Schema(requiredProperties={"actionId","submissionId","partyId","productId","offerId","offerVersion","acceptedControlGeneration","acceptanceActionId","acceptanceIntentDigest","termsDigest","contractId","policyNumber","inceptionDate","expiryDate"})
public record GovernedPolicyBindRequest(UUID actionId,UUID submissionId,UUID partyId,UUID productId,
        UUID offerId,long offerVersion,long acceptedControlGeneration,UUID acceptanceActionId,
        String acceptanceIntentDigest,String termsDigest,UUID contractId,String policyNumber,LocalDate inceptionDate,LocalDate expiryDate) {
    public GovernedPolicyBindRequest {
        GovernedPolicyValues.id(actionId);GovernedPolicyValues.id(submissionId);GovernedPolicyValues.id(partyId);GovernedPolicyValues.id(productId);
        GovernedPolicyValues.id(offerId);GovernedPolicyValues.id(acceptanceActionId);GovernedPolicyValues.id(contractId);
        GovernedPolicyValues.digest(acceptanceIntentDigest);GovernedPolicyValues.digest(termsDigest);GovernedPolicyValues.text(policyNumber,64);
        Objects.requireNonNull(inceptionDate);Objects.requireNonNull(expiryDate);
        if(offerVersion<1 || acceptedControlGeneration<1 || inceptionDate.getYear()<1 || expiryDate.getYear()>9999 || !expiryDate.isAfter(inceptionDate))throw new IllegalArgumentException("Exact accepted offer and coverage selectors required");
    }
    @Override public String toString(){return "GovernedPolicyBindRequest[redacted]";}
}
