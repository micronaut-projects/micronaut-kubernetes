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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.cache.Cache;

/**
 * Informer factory interface.
 */
public interface SharedIndexInformerFactory {

    /**
     * Creates a new {@link SharedIndexInformer}.
     *
     * @param apiTypeClass the api type class
     * @param namespace    the namespace
     * @param <ApiType>    api type
     * @return instance of {@link SharedIndexInformer}
     */
    <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        @Nullable String namespace);

    /**
     * Creates a new {@link SharedIndexInformer}.
     *
     * @param apiTypeClass       the api type class
     * @param namespace          the namespace
     * @param resyncPeriodMillis the resync period
     * @param <ApiType>          api type
     * @return instance of {@link SharedIndexInformer}
     */
    <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        @Nullable String namespace,
        long resyncPeriodMillis);

    /**
     * Creates a new {@link SharedIndexInformer}.
     *
     * @param apiTypeClass       the api type class
     * @param namespace          the namespace
     * @param resyncPeriodMillis the resync period
     * @param cache              the cache
     * @param <ApiType>          api type
     * @return instance of {@link SharedIndexInformer}
     */
    <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        @Nullable String namespace,
        long resyncPeriodMillis,
        Cache<ApiType> cache);

    /**
     * Returns already created {@link SharedIndexInformer}.
     *
     * @param apiTypeClass the api type class
     * @param namespace    the namespace
     * @param <ApiType>    api type
     * @return instance of {@link SharedIndexInformer}
     */
    <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> getExistingSharedIndexInformer(
        Class<ApiType> apiTypeClass,
        String namespace);

    /**
     * Starts all registered informers.
     */
    void startAllRegisteredInformers();

    /**
     * Stops all registered informers.
     */
    void stopAllRegisteredInformers();
}
