/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.client.openapi.operator.leaderelection;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.Date;

@Serdeable
public record LeaderElectionRecord(
    @Nullable String holderIdentity,
    int leaseDurationSeconds,
    @Nullable Date acquireTime,
    @Nullable Date renewTime,
    int leaderTransitions
) {
    public LeaderElectionRecord() {
        this(null, 0, null, null, 0);
    }
}
