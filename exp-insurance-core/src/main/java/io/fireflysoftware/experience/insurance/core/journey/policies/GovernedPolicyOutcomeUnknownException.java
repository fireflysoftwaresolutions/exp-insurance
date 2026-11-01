package io.fireflysoftware.experience.insurance.core.journey.policies;

/** Emitted only after a Domain mutation may have been dispatched. */
public final class GovernedPolicyOutcomeUnknownException extends RuntimeException {
    public GovernedPolicyOutcomeUnknownException(){super("Original governed policy action requires Domain recovery");}
}
