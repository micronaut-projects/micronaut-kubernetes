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

import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.discovery.AbstractKubernetesServiceInstanceProvider;
import io.micronaut.kubernetes.discovery.KubernetesServiceConfiguration;
import io.micronaut.kubernetes.util.KubernetesUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service instance provider uses Kubernetes Endpoints as source of service discovery.
 *
 * @author Pavol Gressa
 * @since 3.2
 */
public abstract class AbstractV1EndpointsProvider extends AbstractKubernetesServiceInstanceProvider {
    public static final String MODE = "endpoint";
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractV1EndpointsProvider.class);

    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;

    public AbstractV1EndpointsProvider(KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
        this.discoveryConfiguration = discoveryConfiguration;
    }

    @Override
    public String getMode() {
        return MODE;
    }

    @Override
    public Publisher<String> getServiceIds(String namespace) {
        return listEndpoints(namespace)
                .filter(discoveryConfigurationFilter(discoveryConfiguration))
                .mapNotNull(KubernetesUtils::objectNameOrNull)
                .filter(Objects::nonNull);
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(KubernetesServiceConfiguration serviceConfiguration) {
        String serviceName = serviceConfiguration.getName().orElseThrow(
                () -> new IllegalArgumentException("KubernetesServiceConfiguration is missing service name."));
        String serviceNamespace = serviceConfiguration.getNamespace().orElseThrow(
                () -> new IllegalArgumentException("KubernetesServiceConfiguration is missing namespace."));

        AtomicReference<V1ObjectMeta> metadata = new AtomicReference<>();

        Mono<V1Endpoints> v1EndpointsMono;

        return getEndpoints(serviceName, serviceNamespace)
                .filter(serviceConfigurationDiscoveryFilter(serviceConfiguration, discoveryConfiguration))
                .doOnNext(endpoints -> metadata.set(endpoints.getMetadata()))
                .mapNotNull(V1Endpoints::getSubsets)
                .flatMapIterable(Function.identity())
                .filter(subset ->
                        hasValidPortConfiguration(Optional.ofNullable(subset.getPorts()).orElse(new ArrayList<>()).stream().map(PortBinder::fromEndpointPort).collect(Collectors.toList()), serviceConfiguration))
                .filter(subset ->
                        subset.getAddresses() != null && !subset.getAddresses().isEmpty())
                .map(subset -> Optional.ofNullable(subset.getPorts()).orElse(new ArrayList<>())
                        .stream()
                        .filter(port -> !serviceConfiguration.getPort().isPresent() || Objects.equals(port.getName(), serviceConfiguration.getPort().get()))
                        .flatMap(port -> subset.getAddresses()
                                .stream()
                                .map(address -> buildServiceInstance(serviceConfiguration.getServiceId(), PortBinder.fromEndpointPort(port), address.getIp(), metadata.get())))
                        .collect(Collectors.toList()))
                .onErrorResume(throwable -> {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Error while processing discovered endpoints [" + serviceName + "]", throwable);
                    }
                    return Flux.just(Collections.emptyList());
                })
                .defaultIfEmpty(new ArrayList<>());
    }

    public abstract Mono<V1Endpoints> getEndpoints(String name, String namespace);

    public abstract Flux<V1Endpoints> listEndpoints(String namespace);
}
