/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.kubernetes.client.v1.pods;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.kubernetes.client.v1.Metadata;

/**
 * Represents a Kubernetes Pod.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Introspected
public class Pod {

    private Metadata metadata;
    private PodStatus status;

    /**
     * @return metadata
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * @param metadata metadata
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * @return pod status information
     */
    public PodStatus getStatus() {
        return status;
    }

    /**
     * @param podStatus pod status information
     */
    public void setStatus(PodStatus podStatus) {
        this.status = podStatus;
    }

    @Override
    public String toString() {
        return "Pod{" +
                "metadata=" + metadata +
                ", status=" + status +
                '}';
    }
}
