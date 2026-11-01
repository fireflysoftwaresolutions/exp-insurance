package io.fireflysoftware.experience.insurance.web.policies;

import io.fireflysoftware.experience.insurance.core.journey.policies.GovernedPolicyDomainPort.Action;
import java.net.URI;
import java.time.Duration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import reactor.core.publisher.Mono;

/** A caller-owned lease for the genuine generated Domain SDK adapter; it performs no business request. */
public interface GovernedPolicyDomainClientFactory {
    Mono<Client> current(Action action);
    interface Client {
        Mono<Void> revalidate();
        Duration remaining();
        String bearerToken();
        URI endpoint();
        ClientHttpConnector connector();
    }
}
