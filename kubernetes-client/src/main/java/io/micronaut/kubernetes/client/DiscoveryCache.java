/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.kubernetes.client;

import io.kubernetes.client.Discovery;
import io.kubernetes.client.apimachinery.GroupVersionKind;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Bean that provides caching over the {@link Discovery} results.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
@Requires(beans = Discovery.class)
@Singleton
@Internal
public class DiscoveryCache {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryCache.class);

    private static final ModelMapper MODEL_MAPPER = new ModelMapper();

    private final Provider<Discovery> discovery;
    private final Duration refreshInterval;

    private Set<Discovery.APIResource> lastAPIDiscovery = new HashSet<>();
    private volatile long nextDiscoveryRefreshTimeMillis = 0;

    /**
     * Create a discovery cache.
     *
     * @param discovery the discovery object to cache
     * @param apiDiscoveryCacheConfiguration the cache configuration
     *
     * @deprecated Moved to use the lazy constructor, see {@link DiscoveryCache#DiscoveryCache(Provider, ApiClientConfiguration.ApiDiscoveryCacheConfiguration)}
     */
    @Deprecated
    public DiscoveryCache(Discovery discovery,
                          ApiClientConfiguration.ApiDiscoveryCacheConfiguration apiDiscoveryCacheConfiguration) {
        this(() -> discovery, apiDiscoveryCacheConfiguration);
    }

    /**
     * Create a discovery cache.
     *
     * @param discovery A provider for the discovery object to cache
     * @param apiDiscoveryCacheConfiguration the cache configuration
     *
     * @since 3.4.0
     */
    @Inject
    public DiscoveryCache(Provider<Discovery> discovery,
                          ApiClientConfiguration.ApiDiscoveryCacheConfiguration apiDiscoveryCacheConfiguration) {
        this.discovery = discovery;
        this.refreshInterval = Duration.ofMinutes(apiDiscoveryCacheConfiguration.getRefreshInterval());
    }

    /**
     * Find all {@link Discovery.APIResource}.
     *
     * @return set of discovered resources
     * @throws ApiException when failed to download api resources
     */
    public Set<Discovery.APIResource> findAll() throws ApiException {
        return getLastAPIDiscovery();
    }

    /**
     * Finds the {@link Discovery.APIResource} for respective {@link KubernetesObject} class.
     *
     * @param clazz kubernetes object class
     * @return api resource
     */
    public Optional<Discovery.APIResource> find(Class<? extends KubernetesObject> clazz) {
        GroupVersionKind groupVersionKind = MODEL_MAPPER.getGroupVersionKindByClass(clazz);
        try {
            return getLastAPIDiscovery().stream()
                    .filter(r -> r.getKind().equalsIgnoreCase(groupVersionKind.getKind()))
                    .findFirst();
        } catch (ApiException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to find api resource information for {}: {}", e.getResponseBody(),
                        groupVersionKind.getKind(), e);
            }
            return Optional.empty();
        }
    }

    @Retryable
    private Set<Discovery.APIResource> getLastAPIDiscovery() throws ApiException {
        long nowMillis = System.currentTimeMillis();
        if (nowMillis < nextDiscoveryRefreshTimeMillis) {
            return lastAPIDiscovery;
        }

        lastAPIDiscovery = discovery.get().findAll();
        nextDiscoveryRefreshTimeMillis = refreshInterval.toMillis() + nowMillis;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Successfully fetched {} Api resources, next fetch will happen after {} minutes",
                    lastAPIDiscovery.size(), refreshInterval.toMinutes());
        }
        return lastAPIDiscovery;
    }
}
