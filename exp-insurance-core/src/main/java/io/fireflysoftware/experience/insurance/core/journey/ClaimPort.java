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
 * Port for the claims saga. Deny is an insurer action and is not on this customer port.
 */
public interface ClaimPort {

    Mono<InsuranceRecord> create(ClaimCommand command, ClaimStatus defaultStatus);

    Mono<InsuranceRecord> get(UUID claimId);

    Mono<InsuranceRecord> update(UUID claimId, ClaimCommand command);

    Mono<InsuranceRecord> status(UUID claimId, ClaimStatus status, String reason);

    Mono<InsuranceRecord> close(UUID claimId, String reason);
}
