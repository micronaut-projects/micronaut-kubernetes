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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
    @NonNull <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
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
    @NonNull <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
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
     * @param waitForInitialSync if set to {@code true}, the {@link #startAllRegisteredInformers()} method will block until existing kubernetes objects
     *                           of given type (in given namespace and filtered by given label selector if provided) get loaded into
     *                           the informer's in-memory storage or predefined timeout ({@link InformerConfiguration#getSyncTimeout()}) gets expired
     * @param <ApiType>          kubernetes api type
     * @return instance of {@link SharedIndexInformer}
     */
    @NonNull <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace,
        @Nullable String labelSelector,
        boolean waitForInitialSync);

    /**
     * Creates a new {@link SharedIndexInformer}.
     *
     * @param apiTypeClass       the api type class
     * @param namespace          the namespace should be set to {@code null} for cluster-wide objects (e.g. V1Node) or
     *                           for namespaced objects (e.g. V1Secret) when the informer needs to handle kubernetes objects from all namespaces
     * @param labelSelector      the <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">label selector</a>
     * @param waitForInitialSync if set to {@code true}, the {@link #startAllRegisteredInformers()} method will block until existing kubernetes objects
     *                           of given type (in given namespace and filtered by given label selector if provided) get loaded into
     *                           the informer's in-memory storage or predefined timeout ({@link InformerConfiguration#getSyncTimeout()}) gets expired
     * @param resyncPeriodMillis the listener default resync period in milliseconds
     * @param <ApiType>          kubernetes api type
     * @return instance of {@link SharedIndexInformer}
     */
    @NonNull <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace,
        @Nullable String labelSelector,
        boolean waitForInitialSync,
        long resyncPeriodMillis);

    /**
     * Creates a new {@link SharedIndexInformer}.
     *
     * @param apiTypeClass        the api type class
     * @param namespace           the namespace should be set to {@code null} for cluster-wide objects (e.g. V1Node) or
     *                            for namespaced objects (e.g. V1Secret) when the informer needs to handle kubernetes objects from all namespaces
     * @param labelSelector       the <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">label selector</a>
     * @param waitForInitialSync  if set to {@code true}, the {@link #startAllRegisteredInformers()} method will block until existing kubernetes objects
     *                            of given type (in given namespace and filtered by given label selector if provided) get loaded into
     *                            the informer's in-memory storage or predefined timeout ({@link InformerConfiguration#getSyncTimeout()}) gets expired
     * @param resyncPeriodMillis  the listener default resync period in milliseconds
     * @param cacheKeyFunction    the function that is used to create cache key
     * @param cacheIndexFunctions the map of functions used to create indexes
     * @param <ApiType>           api type
     * @return instance of {@link SharedIndexInformer}
     */
    @NonNull <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace,
        @Nullable String labelSelector,
        boolean waitForInitialSync,
        long resyncPeriodMillis,
        @Nullable Function<ApiType, String> cacheKeyFunction,
        @Nullable Map<String, Function<ApiType, List<String>>> cacheIndexFunctions);

    /**
     * Creates a new {@link SharedIndexInformer} for each namespace.
     *
     * @param apiTypeClass       the api type class
     * @param namespaces         the list of namespaces
     * @param labelSelector      the <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">label selector</a>
     * @param waitForInitialSync if set to {@code true}, the {@link #startAllRegisteredInformers()} method will block until existing kubernetes objects
     *                           of given type (in given namespace and filtered by given label selector if provided) get loaded into
     *                           the informer's in-memory storage or predefined timeout ({@link InformerConfiguration#getSyncTimeout()}) gets expired
     * @param resyncPeriodMillis the listener default resync period in milliseconds
     * @param <ApiType>          kubernetes api type
     * @return list of {@link SharedIndexInformer} instances. The order of informers in the returned list matches the order of namespaces in the input namespace list.
     */
    @NonNull <ApiType extends KubernetesObject> List<SharedIndexInformer<ApiType>> sharedIndexInformersFor(
        @NonNull Class<ApiType> apiTypeClass,
        @NonNull List<String> namespaces,
        @Nullable String labelSelector,
        boolean waitForInitialSync,
        long resyncPeriodMillis);

    /**
     * Returns already created {@link SharedIndexInformer}.
     *
     * @param apiTypeClass the api type class
     * @param namespace    the namespace
     * @param <ApiType>    api type
     * @return instance of {@link SharedIndexInformer}
     */
    @Nullable <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> getExistingSharedIndexInformer(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace);

    /**
     * Starts all registered informers. If there are informers created with {@code waitForInitialSync=true},
     * the method will block until in-memory storage of those informers get synced (existing kubernetes object loaded)
     * or predefined timeout ({@link InformerConfiguration#getSyncTimeout()}) gets expired.
     */
    void startAllRegisteredInformers();

    /**
     * Stops all registered informers.
     */
    void stopAllRegisteredInformers();
}
