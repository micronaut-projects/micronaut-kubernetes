/*
 * Copyright 2021 original authors
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


import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.util.List;

@Requires(property = "spec.name", value = "ConfigMapInformerSpec")
//tag::cache[]
@Singleton
public class SharedInformerCache {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;

    public SharedInformerCache(SharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
    }

    /**
     * Get all config maps from informer from namespace.
     */
    List<V1ConfigMap> getConfigMaps(String namespace){
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = sharedIndexInformerFactory.getExistingSharedIndexInformer(namespace, V1ConfigMap.class);
        Indexer<V1ConfigMap> indexer = sharedIndexInformer.getIndexer();
        return indexer.list();
    }
}
//end::cache[]
