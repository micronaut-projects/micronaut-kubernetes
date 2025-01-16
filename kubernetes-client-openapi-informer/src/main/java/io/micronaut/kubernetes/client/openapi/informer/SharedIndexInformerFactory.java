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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer;

import java.util.List;

/**
 * Informer factory interface.
 */
public interface SharedIndexInformerFactory {

    /**
     * Creates a new {@link SharedIndexInformer}.
     *
     * @param apiTypeClass the api type class
     * @param namespace    the namespace should be set to {@code null} for cluster-wide objects (e.g. V1Node) or
     *                     for namespaced objects (e.g. V1Secret) when the informer needs to handle kubernetes objects from all namespaces
     * @param <ApiType>    kubernetes api type
     * @return instance of {@link SharedIndexInformer}
     */
    <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace);

    /**
     * Creates a new {@link SharedIndexInformer}.
     *
     * @param apiTypeClass  the api type class
     * @param namespace     the namespace should be set to {@code null} for cluster-wide objects (e.g. V1Node) or
     *                      for namespaced objects (e.g. V1Secret) when the informer needs to handle kubernetes objects from all namespaces
     * @param labelSelector the <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">label selector</a>
     * @param <ApiType>     kubernetes api type
     * @return instance of {@link SharedIndexInformer}
     */
    <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace,
        @Nullable String labelSelector);

    /**
     * Creates a new {@link SharedIndexInformer}.
     *
     * @param apiTypeClass       the api type class
     * @param namespace          the namespace should be set to {@code null} for cluster-wide objects (e.g. V1Node) or
     *                           for namespaced objects (e.g. V1Secret) when the informer needs to handle kubernetes objects from all namespaces
     * @param labelSelector      the <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">label selector</a>
     * @param resyncPeriodMillis the resync period in milliseconds
     * @param <ApiType>          kubernetes api type
     * @return instance of {@link SharedIndexInformer}
     */
    <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace,
        @Nullable String labelSelector,
        long resyncPeriodMillis);

    /**
     * Creates a new {@link SharedIndexInformer}.
     *
     * @param apiTypeClass       the api type class
     * @param namespace          the namespace should be set to {@code null} for cluster-wide objects (e.g. V1Node) or
     *                           for namespaced objects (e.g. V1Secret) when the informer needs to handle kubernetes objects from all namespaces
     * @param labelSelector      the <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">label selector</a>
     * @param resyncPeriodMillis the resync period in milliseconds
     * @param indexer            the indexer
     * @param <ApiType>          api type
     * @return instance of {@link SharedIndexInformer}
     */
    <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace,
        @Nullable String labelSelector,
        long resyncPeriodMillis,
        @Nullable Indexer<ApiType> indexer);

    /**
     * Creates a new {@link SharedIndexInformer} for each namespace.
     *
     * @param apiTypeClass       the api type class
     * @param namespaces         the list of namespaces
     * @param labelSelector      the <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">label selector</a>
     * @param resyncPeriodMillis the resync period
     * @param <ApiType>          kubernetes api type
     * @return list of {@link SharedIndexInformer} instances. The order of informers in the returned list matches the order of namespaces in the input namespace list.
     */
    <ApiType extends KubernetesObject> List<SharedIndexInformer<ApiType>> sharedIndexInformersFor(
        @NonNull Class<ApiType> apiTypeClass,
        @NonNull List<String> namespaces,
        @Nullable String labelSelector,
        long resyncPeriodMillis);

    /**
     * Returns already created {@link SharedIndexInformer}.
     *
     * @param apiTypeClass the api type class
     * @param namespace    the namespace
     * @param <ApiType>    api type
     * @return instance of {@link SharedIndexInformer}
     */
    <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> getExistingSharedIndexInformer(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace);

    /**
     * Starts all registered informers.
     */
    void startAllRegisteredInformers();

    /**
     * Stops all registered informers.
     */
    void stopAllRegisteredInformers();
}
