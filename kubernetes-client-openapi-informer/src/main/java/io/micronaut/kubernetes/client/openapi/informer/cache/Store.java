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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Interface for implementations which stores objects.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/util/src/main/java/io/kubernetes/client/informer/cache/Store.java">Store</a>
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
public interface Store<ApiType> {

    /**
     * Returns a function which creates an object key.
     *
     * @return function which creates an object key
     */
    @NonNull
    Function<ApiType, String> getKeyFunction();

    /**
     * Inserts an item into the store.
     *
     * @param obj specific obj
     */
    void add(@NonNull ApiType obj);

    /**
     * Sets an item in the store to its updated state.
     *
     * @param obj specific obj
     */
    void update(@NonNull ApiType obj);

    /**
     * Removes an item from the store.
     *
     * @param obj specific obj
     */
    void delete(@NonNull ApiType obj);

    /**
     * Replace the content in the cache completely.
     *
     * @param objects list of kubernetes objects
     */
    void replace(@NonNull List<ApiType> objects);

    /**
     * Returns a list of keys of all objects that are currently in the store.
     *
     * @return list of all keys
     */
    @NonNull
    List<String> listKeys();

    /**
     * Returns the request item with specific key.
     *
     * @param key specific key
     * @return the request item
     */
    @Nullable
    ApiType getByKey(@NonNull String key);

    /**
     * Returns a list of all the items.
     *
     * @return list of all the items
     */
    @NonNull
    List<ApiType> list();
}
