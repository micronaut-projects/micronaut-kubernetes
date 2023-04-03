/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.kubernetes.discovery.provider;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service instance provider uses Kubernetes Service API as source of service discovery.
 *
 * @author Pavol Gressa
 * @since 2.3
 */
@Requires(property = "kubernetes.client.discovery.mode-configuration.service.watch.enabled", notEquals = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@Singleton
public class KubernetesServiceInstanceServiceProvider extends AbstractV1ServiceProvider {
    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceServiceProvider.class);

    private final CoreV1ApiReactorClient client;

    /**
     * Creates kubernetes instance service provider.
     *
     * @param client                 client
     * @param discoveryConfiguration discovery configuration
     */
    public KubernetesServiceInstanceServiceProvider(CoreV1ApiReactorClient client,
                                                    KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
        super(discoveryConfiguration);
        this.client = client;
    }

    @Override
    public Mono<V1Service> getService(String name, String namespace) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Using API to fetch Service[{}] from namespace [{}]", name, namespace);
        }

        return client.readNamespacedService(name, namespace, null)
                .doOnError(ApiException.class, throwable -> LOG.error("Failed to fetch Service [" + name + "] from namespace [" + namespace + "]: " + throwable.getResponseBody(), throwable));
    }

    @Override
    public Flux<V1Service> listServices(String namespace) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Using API to fetch services from namespace [{}]", namespace);
        }

        return client.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null)
                .doOnError(ApiException.class, throwable -> LOG.error("Failed to list Services from namespace [" + namespace + "]: " + throwable.getResponseBody(), throwable))
                .flatMapIterable(V1ServiceList::getItems);
    }
}
