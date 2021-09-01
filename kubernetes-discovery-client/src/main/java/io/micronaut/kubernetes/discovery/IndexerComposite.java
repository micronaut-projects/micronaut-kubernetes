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
package io.micronaut.kubernetes.discovery;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.cache.Indexer;
import io.micronaut.core.annotation.NonNull;
import org.apache.commons.collections4.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@link io.kubernetes.client.informer.cache.Indexer} composite for given {@code ApiType} that provides
 * access to the {@link io.kubernetes.client.informer.cache.Store} resources.
 *
 * @param <ApiType> api type of the cache
 * @author Pavol Gressa
 * @since 3.1
 */
public class IndexerComposite<ApiType extends KubernetesObject> {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerComposite.class);

    private final Map<String, Indexer<ApiType>> informerMap = Collections.synchronizedMap(new HashedMap<>());

    public void add(String namespace, Indexer<ApiType> sharedIndexInformer) {
        informerMap.put(namespace, sharedIndexInformer);
    }

    /**
     * Get resource from the {@link io.kubernetes.client.informer.cache.Indexer}.
     *
     * @param name      resource name
     * @param namespace resource namesapce
     * @return mono with the resource or empty mono
     */
    public Mono<ApiType> getResource(@NonNull String name, @NonNull String namespace) {
        Indexer<ApiType> indexer = informerMap.getOrDefault(namespace, null);
        if (indexer == null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Failed to find resource name {} in namespace {}, indexer null", name, namespace);
            }
            return Mono.empty();
        }
        Optional<ApiType> resources = indexer.list().stream().filter(e -> {
            if (e.getMetadata() != null) {
                return Objects.equals(e.getMetadata().getName(), name);
            }
            return false;
        }).findFirst();

        return resources.map(Mono::just).orElseGet(Mono::empty);
    }

    /**
     * Get all resources from the {@link io.kubernetes.client.informer.cache.Indexer} for given {@code namespace}.
     *
     * @param namespace namespace name or null to get resources from all namespaces
     * @return mono with resources or empty mono
     */
    public Flux<ApiType> getResources(@NonNull String namespace) {

        Indexer<ApiType> indexed = informerMap.getOrDefault(namespace, null);
        if (indexed == null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Failed to find resources for namespace {}, indexer null", namespace);
            }
            return Flux.empty();
        }

        return Flux.fromIterable(indexed.list());
    }
}
