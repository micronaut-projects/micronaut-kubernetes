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
package io.micronaut.kubernetes.client.openapi.operator.leaderelection.resourcelock;

import io.micronaut.core.annotation.Internal;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LeaderElectionRecord;

import java.io.IOException;

/**
 * Interface for kubernetes object lock implementation. It is strictly used by the leader election code.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/leaderelection/Lock.java">Lock</a>
 * </p>
 */
@Internal
public sealed interface Lock permits AbstractLock {

    /**
     * Loads the leader election record.
     *
     * @return the leader election record
     */
    LeaderElectionRecord get() throws IOException;

    /**
     * Stores the leader election record.
     *
     * @param leaderElectionRecord the leader election record
     * @return {@code true} if the record has been successfully stored
     */
    boolean create(LeaderElectionRecord leaderElectionRecord);

    /**
     * Updates the leader election record.
     *
     * @param leaderElectionRecord the leader election record
     * @return {@code true} if the record has been successfully updated
     */
    boolean update(LeaderElectionRecord leaderElectionRecord);

    /**
     * Returns the identity of this service instance.
     *
     * @return the identity
     */
    String getIdentity();
}
