package io.fireflysoftware.experience.insurance.web.policies;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Explicit receiver/Domain target binding; Domain retains ownership of its resource dataset scope. */
@ConfigurationProperties("insurance.governed.policy-domain-client")
public record GovernedPolicyDomainClientSettings(String issuer,URI jwkSetUri,String experienceAudience,String inboundClientId,
        String signedTenant,Instant notAfter,
        URI endpoint,String targetAudience,String delegatingClientId,String registrationId) {
    public GovernedPolicyDomainClientSettings {
        issuer=text(issuer);https(URI.create(issuer));https(jwkSetUri);https(endpoint);
        experienceAudience=text(experienceAudience);inboundClientId=text(inboundClientId);signedTenant=text(signedTenant);
        targetAudience=text(targetAudience);delegatingClientId=text(delegatingClientId);registrationId=text(registrationId);
        Objects.requireNonNull(notAfter);if(experienceAudience.equals(targetAudience))throw invalid();
    }
    static URI https(URI value){
        if(value==null || !"https".equals(value.getScheme()) || value.getHost()==null || value.getRawUserInfo()!=null
                || value.getRawQuery()!=null || value.getRawFragment()!=null || !value.normalize().equals(value))throw invalid();return value;
    }
    static String text(String value){
        if(value==null || value.isBlank() || value.length()>512 || value.codePoints().anyMatch(Character::isISOControl))throw invalid();return value;
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Explicit governed policy client configuration required");}
    @Override public String toString(){return "GovernedPolicyDomainClientSettings[redacted]";}
}
