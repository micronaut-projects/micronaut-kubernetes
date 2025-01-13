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
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer;

/*
 * Extends shared informer with indexer.
 *
 * <p>
 * This has been taken and slightly modified from the official client:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/util/src/main/java/io/kubernetes/client/informer/SharedIndexInformer.java">SharedIndexInformer</a>
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
public interface SharedIndexInformer<ApiType extends KubernetesObject> extends SharedInformer<ApiType> {

    /**
     * Returns the internal indexer store.
     *
     * @return the internal indexer store
     */
    Indexer<ApiType> getIndexer();
}
