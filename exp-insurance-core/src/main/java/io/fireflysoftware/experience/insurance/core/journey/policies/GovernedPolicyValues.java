package io.fireflysoftware.experience.insurance.core.journey.policies;

import java.util.UUID;

/** Local shape checks only; Experience never computes the owner's intent hash. */
public final class GovernedPolicyValues {
    private GovernedPolicyValues(){}
    public static UUID id(UUID value){if(value==null || value.equals(new UUID(0,0)))throw invalid();return value;}
    public static String digest(String value){if(value==null || !value.matches("[0-9a-f]{64}"))throw invalid();return value;}
    public static String text(String value,int limit){
        if(value==null || value.isBlank() || value.length()>limit)throw invalid();
        for(int index=0;index<value.length();index++){
            char current=value.charAt(index);if(Character.isISOControl(current))throw invalid();
            if(Character.isHighSurrogate(current)){if(++index>=value.length() || !Character.isLowSurrogate(value.charAt(index)))throw invalid();}
            else if(Character.isLowSurrogate(current))throw invalid();
        }
        return value;
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Exact governed policy selector required");}
}
