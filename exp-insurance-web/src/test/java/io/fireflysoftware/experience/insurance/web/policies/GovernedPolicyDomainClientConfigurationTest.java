package io.fireflysoftware.experience.insurance.web.policies;

import io.fireflysoftware.experience.insurance.core.journey.policies.*;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;

class GovernedPolicyDomainClientConfigurationTest {
    @Test void absentEnablementDoesNotBindSettingsOrCreateAnExchangeFactory(){new ApplicationContextRunner().withUserConfiguration(GovernedPolicyDomainClientConfiguration.class).run(context->{assertNull(context.getStartupFailure());assertEquals(0,context.getBeansOfType(GovernedPolicyDomainClientFactory.class).size());assertEquals(0,context.getBeansOfType(GovernedPolicyDomainClientSettings.class).size());assertEquals(0,context.getBeansOfType(GovernedPolicyDomainPort.class).size());assertEquals(0,context.getBeansOfType(GovernedPolicyJourneyService.class).size());});}
    @Test void equalReceiverAndDomainAudiencesAreNotAnExchangeProfile(){assertThrows(IllegalArgumentException.class,()->new GovernedPolicyDomainClientSettings("https://issuer.invalid",URI.create("https://issuer.invalid/jwks"),"shared","interactive","opaque-tenant",Instant.parse("2027-01-01T00:00:00Z"),URI.create("https://domain.invalid"),"shared","exchange-client","exchange-registration"));}
    @Test void plainHttpEndpointsCannotActivateAClient(){assertThrows(IllegalArgumentException.class,()->new GovernedPolicyDomainClientSettings("https://issuer.invalid",URI.create("https://issuer.invalid/jwks"),"experience","interactive","opaque-tenant",Instant.parse("2027-01-01T00:00:00Z"),URI.create("http://domain.invalid"),"domain","exchange-client","exchange-registration"));}
    @Test void explicitEnabledGraphConstructsWithoutIoAndStillRequiresTheCurrentNativeHuman(){new ApplicationContextRunner().withUserConfiguration(GovernedPolicyDomainClientConfiguration.class)
            .withBean(ReactiveClientRegistrationRepository.class,()->id->Mono.empty()).withPropertyValues(
                "insurance.governed.policy-domain-client.enabled=true","insurance.governed.policy-domain-client.issuer=https://issuer.invalid","insurance.governed.policy-domain-client.jwk-set-uri=https://issuer.invalid/jwks",
                "insurance.governed.policy-domain-client.experience-audience=experience-policy","insurance.governed.policy-domain-client.inbound-client-id=interactive-client","insurance.governed.policy-domain-client.signed-tenant=opaque-platform-namespace",
                "insurance.governed.policy-domain-client.not-after=2099-01-01T00:00:00Z","insurance.governed.policy-domain-client.endpoint=https://domain-policy.invalid","insurance.governed.policy-domain-client.target-audience=domain-policy",
                "insurance.governed.policy-domain-client.delegating-client-id=experience-policy-exchange","insurance.governed.policy-domain-client.registration-id=policy-exchange")
            .run(context->{assertNull(context.getStartupFailure());assertEquals(1,context.getBeansOfType(GovernedPolicyDomainClientFactory.class).size());assertEquals(1,context.getBeansOfType(GovernedPolicyDomainPort.class).size());assertEquals(1,context.getBeansOfType(GovernedPolicyJourneyService.class).size());StepVerifier.create(context.getBean(GovernedPolicyDomainPort.class).current(GovernedPolicyDomainPort.Action.BIND)).expectErrorSatisfies(error->assertEquals(HttpStatus.UNAUTHORIZED,((ResponseStatusException)error).getStatusCode())).verify();});}
}
