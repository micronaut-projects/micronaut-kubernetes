/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.client.informer;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.micronaut.core.annotation.Nullable;

import java.util.List;

/**
 * Informer factory interface.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
public interface SharedIndexInformerFactory {

    /**
     * Creates new {@link SharedIndexInformer}.
     *
     * @param apiTypeClass      api type class
     * @param apiListTypeClass  api list type class
     * @param resourcePlural    resource plural
     * @param namespace         namespace
     * @param labelSelector     label selector
     * @param resyncCheckPeriod resync check period
     * @param <ApiType>         api type
     * @param <ApiListType>     api list type
     * @return list of informers
     */
    <ApiType extends KubernetesObject, ApiListType extends KubernetesListObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
            Class<ApiType> apiTypeClass,
            Class<ApiListType> apiListTypeClass,
            String resourcePlural,
            @Nullable String namespace,
            @Nullable String labelSelector,
            @Nullable Long resyncCheckPeriod);

    /**
     * Creates new {@link SharedIndexInformer}s for every namespace from {@code namespaces} param.
     *
     * @param apiTypeClass      api type class
     * @param apiListTypeClass  api list type class
     * @param resourcePlural    resource plural
     * @param namespaces        namespaces
     * @param labelSelector     label selector
     * @param resyncCheckPeriod resync check period
     * @param <ApiType>         api type
     * @param <ApiListType>     api list type
     * @return list of informers
     */
    <ApiType extends KubernetesObject, ApiListType extends KubernetesListObject> List<SharedIndexInformer<? extends KubernetesObject>> sharedIndexInformersFor(
            Class<ApiType> apiTypeClass,
            Class<ApiListType> apiListTypeClass,
            String resourcePlural,
            @Nullable List<String> namespaces,
            @Nullable String labelSelector,
            @Nullable Long resyncCheckPeriod);

    /**
     * Get existing {@link SharedIndexInformer}.
     *
     * @param namespace    namespace
     * @param apiTypeClass api type class
     * @param <ApiType> api type
     * @return shared index informer or null if not found
     */
    <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> getExistingSharedIndexInformer(String namespace, Class<ApiType> apiTypeClass);

    /**
     * Get all existing {@link SharedIndexInformer}.
     *
     * @return list of existing shared informers
     */
    List<SharedIndexInformer> getExistingSharedIndexInformers();

    /**
     * Start all registered informers.
     */
    void startAllRegisteredInformers();

    /**
     * Stop all registered informers.
     */
    void stopAllRegisteredInformers();
}
