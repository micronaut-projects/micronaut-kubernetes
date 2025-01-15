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

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.exception.ObjectTransformException;

/**
 * TransformFunc allows for transforming an object before it will be processed
 * and put into the cache and before the corresponding handlers will be called on it.
 * TransformFunc (similarly to ResourceEventHandler functions) should be able
 * to correctly handle the tombstone of type DeletedFinalStateUnknown.
 * <p>
 * The most common usage pattern is to clean up some parts of the object to
 * reduce component memory usage if a given component doesn't care about them.
 * </p>
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/util/src/main/java/io/kubernetes/client/informer/TransformFunc.java">TransformFunc</a>
 * </p>
 */
public interface TransformFunc {

    /**
     * Transforms given object.
     *
     * @param object the original object to be transformed
     * @return the transformed object
     */
    KubernetesObject transform(KubernetesObject object) throws ObjectTransformException;
}
