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

import java.io.IOException;

/**
 * Lock offers a common interface for locking on arbitrary resources used in leader election. The
 * Lock is used to hide the details on specific implementations in order to allow them to change
 * over time. This interface is strictly for use by the leaderelection code.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/leaderelection/Lock.java">Lock</a>
 * </p>
 */
public interface Lock {

    /**
     * get returns the LeaderElectionRecord which contains the information about the current lock
     * holder
     *
     * @return the Leader election record
     */
    LeaderElectionRecord get() throws IOException;

    /**
     * create attempts to create a LeaderElectionRecord
     *
     * @param record the information of this candidate
     * @return the result is successful or not
     */
    boolean create(LeaderElectionRecord record);

    /**
     * update attempts to update the existing LeaderElectionRecord
     *
     * @param record the information of this candidate
     * @return the result is successful or not
     */
    boolean update(LeaderElectionRecord record);

    /**
     * identity returns the locks Identity
     *
     * @return the identity
     */
    String getIdentity();

    /**
     * Describe is used to convert details on current resource lock into a string
     *
     * @return the description
     */
    String describe();
}
