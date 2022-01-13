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
package io.micronaut.kubernetes.client.operator;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.util.Strings;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.informer.SharedIndexInformerFactory;
import org.apache.commons.collections4.map.HashedMap;

import java.util.Map;
import java.util.Optional;

/**
 * Operator lister simplifies retrieval of the resources from within the {@link SharedIndexInformer}'s
 * {@link io.kubernetes.client.informer.cache.Cache}.
 *
 * @param <ApiType> the api type of the listed resources
 * @author Pavol Gressa
 * @since 3.3
 */
public class OperatorResourceLister<ApiType extends KubernetesObject> {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;
    private final ControllerConfiguration controllerConfiguration;
    private final Map<String, SharedIndexInformer<ApiType>> informerMap;

    public OperatorResourceLister(@NonNull ControllerConfiguration controllerConfiguration,
                                  @NonNull SharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.controllerConfiguration = controllerConfiguration;
        this.informerMap = new HashedMap<>(controllerConfiguration.getNamespaces().size());
    }

    /**
     * Get the kubernetes resource from the {@link SharedIndexInformer}'s {@link io.kubernetes.client.informer.cache.Cache}
     * for the given {@link Request}. The operation returns {@link Optional} for cases when the resource is not present
     * in the cache.
     *
     * @param request the reconciliation request
     * @return optional resource in local cache
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public Optional<ApiType> get(@NonNull Request request) {
        final SharedIndexInformer<ApiType> sharedIndexInformer = informerMap.computeIfAbsent(request.getNamespace(), namespace -> {
            Class<? extends KubernetesObject> apiType = controllerConfiguration.getApiType();
            return (SharedIndexInformer<ApiType>) sharedIndexInformerFactory.getExistingSharedIndexInformer(namespace, apiType);
        });
        final String key = metaNamespaceKeyFunc(request.getNamespace(), request.getName());
        return Optional.ofNullable(sharedIndexInformer.getIndexer().getByKey(key));
    }

    /**
     * This function is a simplification of {@link io.kubernetes.client.informer.cache.Caches#metaNamespaceKeyFunc(KubernetesObject)}
     * that is used as a default implementation of the indexer key function.
     *
     * @param namespace namespace
     * @param name      name
     * @return name of the resource object in the {@link io.kubernetes.client.informer.cache.Cache}.
     */
    static String metaNamespaceKeyFunc(String namespace, String name) {
        if (!Strings.isNullOrEmpty(namespace)) {
            return namespace + "/" + name;
        }
        return name;
    }
}
