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
package io.micronaut.kubernetes.client.openapi.informer;

import io.micronaut.core.annotation.Internal;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import org.jspecify.annotations.Nullable;

/**
 * DeletedFinalStateUnknown is placed into a DeltaFIFO in the case where an object was deleted
 * but the watch deletion event was missed. In this case we don't know the final "resting" state
 * of the object, so there is a chance the included object is stale.
 *
 * @param key the object key
 * @param object the kubernetes object
 * @param <ApiType> kubernetes api type
 */
@Internal
public record DeletedFinalStateUnknown<ApiType extends KubernetesObject>(
    String key,
    @Nullable ApiType object
) implements KubernetesObject {
    @Override
    @Nullable
    public V1ObjectMeta getMetadata() {
        return object == null ? null : object.getMetadata();
    }

    @Override
    @Nullable
    public String getApiVersion() {
        return object == null ? null : object.getApiVersion();
    }

    @Override
    @Nullable
    public String getKind() {
        return object == null ? null : object.getKind();
    }
}
