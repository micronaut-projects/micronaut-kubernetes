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
package io.micronaut.kubernetes.client.openapi.informer.cache;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Indexer extends Store interface and adds index/de-index methods.
 *
 * <p>
 * This has been taken from the official client:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/util/src/main/java/io/kubernetes/client/informer/cache/Indexer.java">Indexer</a>
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
public interface Indexer<ApiType> extends Store<ApiType> {

    /**
     * Retrieve a list of objects that match on the named indexing function.
     *
     * @param indexName specific indexing function
     * @return matched objects
     */
    List<ApiType> index(String indexName, ApiType obj);

    /**
     * Returns a list of object keys by index name and index key.
     *
     * @param indexName the index name
     * @param indexKey  the index key
     * @return the list of kubernetes object keys
     */
    List<String> indexKeys(String indexName, String indexKey);

    /**
     * Returns a list of objects by index name and index key.
     *
     * @param indexName the index name
     * @param indexKey  the index key
     * @return the list of kubernetes objects
     */
    List<ApiType> byIndex(String indexName, String indexKey);

    /**
     * Return the indexers registered with the store.
     *
     * @return registered indexers
     */
    Map<String, Function<ApiType, List<String>>> getIndexers();

    /**
     * Add additional indexers to the store.
     *
     * @param indexers indexers to add
     */
    void addIndexers(Map<String, Function<ApiType, List<String>>> indexers);
}
