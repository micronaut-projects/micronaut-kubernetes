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
package io.micronaut.kubernetes.client.openapi.informer.handler;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import org.jspecify.annotations.NonNull;

/**
 * Interface for event handlers.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/util/src/main/java/io/kubernetes/client/informer/ResourceEventHandler.java">ResourceEventHandler</a>
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
public interface ResourceEventHandler<ApiType extends KubernetesObject> {

    void onAdd(@NonNull ApiType obj);

    void onUpdate(@NonNull ApiType oldObj, @NonNull ApiType newObj);

    void onDelete(@NonNull ApiType obj, boolean deletedFinalStateUnknown);
}
