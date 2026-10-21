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

package io.fireflysoftware.experience.insurance.core.journey;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port for the underwriting saga. A future BPM replaces the HTTP adapter.
 * Experience code does not import the domain workflow or the underwriting core SDK.
 */
public interface UnderwritingPort {

    Mono<InsuranceRecord> create(QuoteCommand command, SubmissionStatus defaultStatus);

    Mono<InsuranceRecord> get(UUID submissionId);

    Mono<InsuranceRecord> update(UUID submissionId, QuoteCommand command);

    Mono<InsuranceRecord> status(UUID submissionId, SubmissionStatus status, String reason);

    Mono<InsuranceRecord> withdraw(UUID submissionId, String reason);
}
