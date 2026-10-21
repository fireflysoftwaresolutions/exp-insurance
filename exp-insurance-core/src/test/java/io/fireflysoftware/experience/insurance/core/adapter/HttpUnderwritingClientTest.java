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

import io.fireflysoftware.experience.insurance.core.journey.LineOfBusiness;
import io.fireflysoftware.experience.insurance.core.journey.QuoteCommand;
import io.fireflysoftware.experience.insurance.core.journey.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUnderwritingClientTest {

    @Test
    void create_postsSubmissionSaga() {
        UUID submissionId = UUID.randomUUID();
        WebClient client = WebClient.builder().exchangeFunction(request -> {
            assertThat(request.method().name()).isEqualTo("POST");
            assertThat(request.url().getPath()).isEqualTo("/api/v1/submissions");
            return Mono.just(ClientResponse.create(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body("{\"submissionId\":\"" + submissionId + "\",\"submissionNumber\":\"Q-1\",\"submissionStatus\":\"RECEIVED\"}")
                    .build());
        }).build();

        QuoteCommand command = new QuoteCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Q-1", LineOfBusiness.MOTOR, null, "USD");

        StepVerifier.create(new HttpInsuranceConfiguration.HttpUnderwritingClient(client)
                        .create(command, SubmissionStatus.RECEIVED))
                .assertNext(record -> {
                    assertThat(record.id()).isEqualTo(submissionId);
                    assertThat(record.reference()).isEqualTo("Q-1");
                    assertThat(record.status()).isEqualTo("RECEIVED");
                })
                .verifyComplete();
    }
}
