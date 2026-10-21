/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.experience.insurance.core.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fireflysoftware.experience.insurance.core.journey.ClaimStatus;
import io.fireflysoftware.experience.insurance.core.journey.InsuranceOperation;
import io.fireflysoftware.experience.insurance.core.journey.JourneyCatalogPort;
import io.fireflysoftware.experience.insurance.core.journey.JourneySettings;
import io.fireflysoftware.experience.insurance.core.journey.PolicyStatus;
import io.fireflysoftware.experience.insurance.core.journey.SubmissionStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Deterministic journey catalog. An installer replaces this class with an adapter
 * that reads domain-core-configuration. The file, not this class, decides which
 * operation is enabled and which default status is used.
 */
@Component
@ConditionalOnProperty(name = "firefly.experience.insurance.catalog", havingValue = "sandbox", matchIfMissing = true)
public class SandboxJourneyCatalogAdapter implements JourneyCatalogPort {

    private final JourneySettings defaults;

    public SandboxJourneyCatalogAdapter() {
        this(read());
    }

    SandboxJourneyCatalogAdapter(JourneySettings defaults) {
        this.defaults = defaults;
    }

    @Override
    public Mono<JourneySettings> current(UUID productId) {
        return Mono.just(defaults);
    }

    private static JourneySettings read() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = new ClassPathResource("insurance-journey-catalog.json").getInputStream()) {
            JsonNode root = mapper.readTree(in);
            EnumSet<InsuranceOperation> enabled = EnumSet.noneOf(InsuranceOperation.class);
            root.get("enabled").forEach(node -> enabled.add(InsuranceOperation.valueOf(node.asText())));
            return new JourneySettings(
                    enabled,
                    SubmissionStatus.valueOf(root.get("defaultSubmissionStatus").asText()),
                    PolicyStatus.valueOf(root.get("defaultPolicyStatus").asText()),
                    ClaimStatus.valueOf(root.get("defaultClaimStatus").asText()));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
