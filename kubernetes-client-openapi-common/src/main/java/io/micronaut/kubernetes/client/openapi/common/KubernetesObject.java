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
package io.micronaut.kubernetes.client.openapi.common;

import io.micronaut.core.annotation.Internal;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;

/**
 * Common accessors for kubernetes object.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/kubernetes/src/main/java/io/kubernetes/client/common/KubernetesObject.java">KubernetesObject</a>
 * </p>
 */
@Internal
public interface KubernetesObject extends KubernetesType {

    /**
     * Gets metadata.
     *
     * <p>ObjectMeta is metadata that all persisted resources must have, which includes all objects
     * users must create.
     *
     * @return the metadata
     */
    V1ObjectMeta getMetadata();
}
