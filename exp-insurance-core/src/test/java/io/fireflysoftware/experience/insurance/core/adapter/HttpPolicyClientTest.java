/*
 * Copyright 2026 Firefly Software Foundation
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
import com.sun.net.httpserver.HttpServer;
import io.fireflysoftware.domain.insurance.policies.sdk.api.PoliciesApi;
import io.fireflysoftware.domain.insurance.policies.sdk.invoker.ApiClient;
import io.fireflysoftware.experience.insurance.core.journey.LineOfBusiness;
import io.fireflysoftware.experience.insurance.core.journey.PolicyCommand;
import io.fireflysoftware.experience.insurance.core.journey.PolicyStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HttpPolicyClientTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicReference<JsonNode> sent = new AtomicReference<>();
    private final UUID policyId = UUID.randomUUID();
    private HttpServer server;
    private HttpInsuranceConfiguration.HttpPolicyClient client;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/policies", exchange -> {
            sent.set(json.readTree(exchange.getRequestBody()));
            byte[] response = ("{\"policyId\":\"" + policyId + "\",\"policyNumber\":\"SYNTHETIC-001\",\"policyStatus\":\"QUOTE\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        ApiClient sdk = new ApiClient();
        sdk.setBasePath("http://127.0.0.1:" + server.getAddress().getPort());
        client = new HttpInsuranceConfiguration.HttpPolicyClient(new PoliciesApi(sdk));
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @EnumSource(LineOfBusiness.class)
    void createSerializesPartyAndPolicyFieldsThroughTheActualGeneratedSdk(LineOfBusiness business) {
        UUID tenant = UUID.randomUUID();
        UUID contract = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        UUID party = UUID.randomUUID();
        var command = new PolicyCommand(tenant, contract, product, party, "SYNTHETIC-001", business,
                PolicyStatus.QUOTE, LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1), "USD");

        StepVerifier.create(client.create(command, PolicyStatus.QUOTE)).assertNext(result -> {
            assertThat(result.id()).isEqualTo(policyId);
            assertThat(result.reference()).isEqualTo("SYNTHETIC-001");
        }).verifyComplete();

        JsonNode request = sent.get();
        assertThat(request).isNotNull();
        assertThat(request.path("partyId").asText()).isEqualTo(party.toString());
        assertThat(request.path("tenantId").asText()).isEqualTo(tenant.toString());
        assertThat(request.path("contractId").asText()).isEqualTo(contract.toString());
        assertThat(request.path("productId").asText()).isEqualTo(product.toString());
        assertThat(request.path("policyNumber").asText()).isEqualTo("SYNTHETIC-001");
        assertThat(request.path("lineOfBusiness").asText()).isEqualTo(business.name());
        assertThat(request.path("policyStatus").asText()).isEqualTo("QUOTE");
        assertThat(request.path("inceptionDate").asText()).isEqualTo("2026-11-01");
        assertThat(request.path("expiryDate").asText()).isEqualTo("2027-11-01");
        assertThat(request.path("currency").asText()).isEqualTo("USD");
    }

    @Test
    void missingExplicitStatusUsesTheConfiguredDefaultInTheTypedRequest() {
        var command = new PolicyCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "SYNTHETIC-001", LineOfBusiness.PROPERTY, null, LocalDate.of(2026, 11, 1), null, "USD");
        StepVerifier.create(client.create(command, PolicyStatus.BOUND)).expectNextCount(1).verifyComplete();
        assertThat(sent.get().path("policyStatus").asText()).isEqualTo("BOUND");
        assertThat(sent.get().path("partyId").asText()).isEqualTo(command.partyId().toString());
    }
}
