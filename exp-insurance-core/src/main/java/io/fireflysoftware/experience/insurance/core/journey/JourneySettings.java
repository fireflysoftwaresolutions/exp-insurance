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

import java.util.EnumSet;

/**
 * Enablement and default statuses loaded from reference data.
 * An installer replaces the sandbox file. The journey does not hardcode which operation is on.
 */
public record JourneySettings(
        EnumSet<InsuranceOperation> enabled,
        SubmissionStatus defaultSubmissionStatus,
        PolicyStatus defaultPolicyStatus,
        ClaimStatus defaultClaimStatus) {

    public boolean enabled(InsuranceOperation operation) {
        return enabled != null && enabled.contains(operation);
    }
}
