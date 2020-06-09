/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.kubernetes.client.v1.pods;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.kubernetes.client.v1.KubernetesObject;

/**
 * Represents a Kubernetes Pod.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Introspected
public class Pod extends KubernetesObject {

    private PodStatus status;
    private PodSpec spec;

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

    /**
     * @return The specification of the desired behavior of the pod
     */
    public PodSpec getSpec() {
        return spec;
    }

    /**
     * @param spec The specification of the desired behavior of the pod
     */
    public void setSpec(final PodSpec spec) {
        this.spec = spec;
    }

    @Override
    public String toString() {
        return "Pod{" +
                "metadata=" + getMetadata() +
                ", status=" + status +
                ", spec=" + spec +
                '}';
    }
}
