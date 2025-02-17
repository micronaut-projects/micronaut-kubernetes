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
package io.micronaut.kubernetes.client.openapi.discovery.provider;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.model.V1Service;
import io.micronaut.kubernetes.client.openapi.model.V1ServiceList;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service instance provider uses Kubernetes Service API as source of service discovery.
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(property = "kubernetes.client.discovery.mode-configuration.service.watch.enabled", notEquals = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
final class KubernetesServiceInstanceServiceProvider extends AbstractV1ServiceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceServiceProvider.class);

    private final CoreV1ApiReactor client;

    KubernetesServiceInstanceServiceProvider(CoreV1ApiReactor client,
                                             KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
        super(discoveryConfiguration);
        this.client = client;
    }

    @Override
    public Mono<V1Service> getService(String name, String namespace) {
        LOG.trace("Using API to fetch [{}] Service from namespace [{}]", name, namespace);
        return client.readNamespacedService(name, namespace, null)
                .doOnError(throwable -> LOG.error("Failed to fetch Service [{}] from namespace [{}]", name, namespace, throwable));
    }

    @Override
    public Flux<V1Service> listServices(String namespace) {
        LOG.trace("Using API to fetch all services from namespace [{}]", namespace);
        return client.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null, null, null)
                .doOnError(throwable -> LOG.error("Failed to list Services from namespace [{}]", namespace, throwable))
                .flatMapIterable(V1ServiceList::getItems);
    }
}
