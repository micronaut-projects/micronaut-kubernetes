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
package io.micronaut.kubernetes.client;

import io.kubernetes.client.Discovery;
import io.kubernetes.client.apimachinery.GroupVersionKind;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
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
public class DiscoveryCache {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryCache.class);

    private static final ModelMapper MODEL_MAPPER = new ModelMapper();

    private final Discovery discovery;
    private final Duration refreshInterval;

    private Set<Discovery.APIResource> lastAPIDiscovery = new HashSet<>();
    private volatile long nextDiscoveryRefreshTimeMillis = 0;

    public DiscoveryCache(Discovery discovery,
                          @Value("${kubernetes.client.api-discovery.cache.refresh-interval:30}") long discoveryRefreshInterval) {
        this.discovery = discovery;
        this.refreshInterval = Duration.ofMinutes(discoveryRefreshInterval);
    }

    /**
     * Find all {@link Discovery.APIResource}.
     *
     * @return set of discovered resources
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

    private Set<Discovery.APIResource> getLastAPIDiscovery() throws ApiException {
        long nowMillis = System.currentTimeMillis();
        if (nowMillis < nextDiscoveryRefreshTimeMillis) {
            return lastAPIDiscovery;
        }

        lastAPIDiscovery = discovery.findAll();
        nextDiscoveryRefreshTimeMillis = refreshInterval.toMillis() + nowMillis;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Successfully fetched {} Api resources, next fetch will happen after {} minutes",
                    lastAPIDiscovery.size(), refreshInterval.toMinutes());
        }
        return lastAPIDiscovery;
    }
}
