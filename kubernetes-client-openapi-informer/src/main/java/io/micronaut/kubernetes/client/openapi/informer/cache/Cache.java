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

import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.DeletedFinalStateUnknown;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Cache is a java port of k/client-go's ThreadSafeStore. It basically saves and indexes all the entries.
 *
 * <p>
 * This has been taken and modified from the official client:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/util/src/main/java/io/kubernetes/client/informer/cache/Cache.java">Cache</a>
 * </p>
 */
public class Cache<ApiType extends KubernetesObject> implements Indexer<ApiType> {

    public static final String DEFAULT_INDEX_NAME = "namespace";

    private final Function<ApiType, String> keyFunction;

    /** indexers stores index functions by index names */
    private final Map<String, Function<ApiType, List<String>>> indexers = new HashMap<>();

    /** items stores object instances by keys created from the key function */
    private final Map<String, ApiType> items = new HashMap<>();

    /** indices stores objects' keys by their indices for each index name */
    private final Map<String, Map<String, Set<String>>> indices = new HashMap<>();

    public Cache() {
        this(Cache::getDefaultKeyFunc);
    }

    public Cache(Function<ApiType, String> keyFunction) {
        this(keyFunction, Collections.singletonMap(DEFAULT_INDEX_NAME, Cache::getDefaultIndexFunc));
    }

    public Cache(Map<String, Function<ApiType, List<String>>> indexFunctions) {
        this(Cache::getDefaultKeyFunc, indexFunctions);
    }

    public Cache(Function<ApiType, String> keyFunction,
                 Map<String, Function<ApiType, List<String>>> indexFunctions) {
        this.keyFunction = keyFunction;
        indexFunctions.forEach((indexName, indexFunc) -> {
            indexers.put(indexName, indexFunc);
            indices.put(indexName, new HashMap<>());
        });
    }

    @Override
    public Function<ApiType, String> getKeyFunction() {
        return keyFunction;
    }

    @Override
    public void add(ApiType object) {
        String key = keyFunction.apply(object);
        synchronized (this) {
            ApiType oldObject = items.put(key, object);
            updateIndices(oldObject, object, key);
        }
    }

    @Override
    public void update(ApiType object) {
        String key = keyFunction.apply(object);
        synchronized (this) {
            ApiType oldObject = items.put(key, object);
            updateIndices(oldObject, object, key);
        }
    }

    @Override
    public void delete(ApiType object) {
        String objectKey = keyFunction.apply(object);
        synchronized (this) {
            ApiType deletedObject = items.remove(objectKey);
            if (deletedObject != null) {
                deleteFromIndices(deletedObject, objectKey);
            }
        }
    }

    @Override
    public synchronized void replace(List<ApiType> objects) {
        items.clear();
        indices.clear();
        objects.forEach(object -> items.put(keyFunction.apply(object), object));
        items.forEach((objectKey, object) -> updateIndices(null, object, objectKey));
    }

    @Override
    public synchronized List<String> listKeys() {
        return List.copyOf(items.keySet());
    }

    @Override
    public synchronized List<ApiType> list() {
        return List.copyOf(items.values());
    }

    @Override
    public synchronized ApiType getByKey(String key) {
        return items.get(key);
    }

    @Override
    public synchronized List<String> indexKeys(String indexName, String indexKey) {
        Map<String, Set<String>> index = indices.get(indexName);
        if (index == null) {
            throw new IllegalArgumentException(String.format("index %s doesn't exist!", indexName));
        }
        Set<String> objectKeys = index.get(indexKey);
        return objectKeys == null ? Collections.emptyList() : new ArrayList<>(objectKeys);
    }

    @Override
    public synchronized List<ApiType> byIndex(String indexName, String indexKey) {
        Map<String, Set<String>> index = indices.get(indexName);
        if (index == null) {
            throw new IllegalArgumentException(String.format("index %s doesn't exist!", indexName));
        }
        Set<String> objectKeys = index.get(indexKey);
        if (objectKeys == null) {
            return Collections.emptyList();
        }
        List<ApiType> objects = new ArrayList<>(objectKeys.size());
        for (String objectKey : objectKeys) {
            objects.add(items.get(objectKey));
        }
        return objects;
    }

    @Override
    public Map<String, Function<ApiType, List<String>>> getIndexers() {
        return Collections.unmodifiableMap(indexers);
    }

    private void updateIndices(ApiType oldObject, ApiType newObject, String objectKey) {
        // if we got an old object, we need to remove it before we can add it again
        if (oldObject != null) {
            deleteFromIndices(oldObject, objectKey);
        }
        indexers.forEach((indexName, indexFunc) -> {
            List<String> indexKeys = indexFunc.apply(newObject);
            if (CollectionUtils.isNotEmpty(indexKeys)) {
                Map<String, Set<String>> index = indices.computeIfAbsent(indexName, k -> new HashMap<>());
                indexKeys.forEach(indexKey ->
                    index.computeIfAbsent(indexKey, k -> new HashSet<>()).add(objectKey)
                );
            }
        });
    }

    private void deleteFromIndices(ApiType oldObj, String objectKey) {
        indexers.forEach((indexName, indexFunc) -> {
            Map<String, Set<String>> index = indices.get(indexName);
            if (index == null) {
                return;
            }
            List<String> indexKeys = indexFunc.apply(oldObj);
            if (CollectionUtils.isEmpty(indexKeys)) {
                return;
            }
            for (String indexKey : indexKeys) {
                Set<String> objectKeys = index.get(indexKey);
                if (objectKeys != null) {
                    objectKeys.remove(objectKey);
                }
            }
        });
    }

    private static String getDefaultKeyFunc(KubernetesObject object) {
        if (object instanceof DeletedFinalStateUnknown<?> deleteObj) {
            return deleteObj.getKey();
        }
        V1ObjectMeta metadata = object.getMetadata();
        return StringUtils.isEmpty(metadata.getNamespace())
            ? metadata.getName()
            : metadata.getNamespace() + "/" + metadata.getName();
    }

    private static List<String> getDefaultIndexFunc(KubernetesObject obj) {
        V1ObjectMeta metadata = obj.getMetadata();
        return metadata == null ? Collections.emptyList() : Collections.singletonList(metadata.getNamespace());
    }
}
