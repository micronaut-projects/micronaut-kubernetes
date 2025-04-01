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
package io.micronaut.kubernetes.client.openapi.operator;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;

import java.util.Optional;

/**
 * Operator lister simplifies retrieval of the resources from within the {@link SharedIndexInformer}'s cache.
 *
 * @param <ApiType> kubernetes api type
 * @author Pavol Gressa
 */
public final class OperatorResourceLister<ApiType extends KubernetesObject> {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;
    private final Class<ApiType> apiTypeClass;
    private final boolean allNamespaces;

    public OperatorResourceLister(@NonNull SharedIndexInformerFactory sharedIndexInformerFactory,
                                  @NonNull Class<ApiType> apiTypeClass,
                                  boolean allNamespaces) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.apiTypeClass = apiTypeClass;
        this.allNamespaces = allNamespaces;
    }

    /**
     * Get the kubernetes resource from the {@link SharedIndexInformer}'s cache for the given {@link Request}.
     * The operation returns {@link Optional} for cases when the resource is not present in the cache.
     *
     * @param request the reconciliation request
     * @return optional resource in local cache
     */
    @NonNull public Optional<ApiType> get(@NonNull Request request) {
        String namespace = request.namespace();
        String name = request.name();
        SharedIndexInformer<ApiType> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            apiTypeClass,
            allNamespaces ? null : namespace);
        String key = StringUtils.isEmpty(namespace) ? name : namespace + "/" + name;
        return Optional.ofNullable(informer.getIndexer().getByKey(key));
    }
}
