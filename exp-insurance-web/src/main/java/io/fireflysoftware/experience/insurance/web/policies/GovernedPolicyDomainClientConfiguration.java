package io.fireflysoftware.experience.insurance.web.policies;

import io.fireflysoftware.experience.insurance.web.policies.impl.ExchangedGovernedPolicyDomainClientFactoryImpl;
import io.fireflysoftware.experience.insurance.web.policies.impl.GeneratedGovernedPolicyDomainPortImpl;
import io.fireflysoftware.experience.insurance.core.journey.policies.GovernedPolicyDomainPort;
import io.fireflysoftware.experience.insurance.core.journey.policies.GovernedPolicyJourneyService;
import io.fireflysoftware.experience.insurance.core.journey.policies.impl.GovernedPolicyJourneyServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="insurance.governed.policy-domain-client.enabled",havingValue="true")
@EnableConfigurationProperties(GovernedPolicyDomainClientSettings.class)
public class GovernedPolicyDomainClientConfiguration {
    @Bean GovernedPolicyDomainClientFactory governedPolicyDomainClientFactory(GovernedPolicyDomainClientSettings settings,ReactiveClientRegistrationRepository registrations){
        return new ExchangedGovernedPolicyDomainClientFactoryImpl(settings,registrations);
    }
    @Bean GovernedPolicyDomainPort governedPolicyDomainPort(GovernedPolicyDomainClientFactory clients){return new GeneratedGovernedPolicyDomainPortImpl(clients);}
    @Bean GovernedPolicyJourneyService governedPolicyJourneyService(GovernedPolicyDomainPort domain){return new GovernedPolicyJourneyServiceImpl(domain);}
}
