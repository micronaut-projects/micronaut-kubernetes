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

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1EndpointsList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.micronaut.context.annotation.Value;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.kubernetes.discovery.AbstractKubernetesServiceInstanceProvider;
import io.micronaut.kubernetes.discovery.KubernetesServiceConfiguration;
import io.micronaut.kubernetes.discovery.IndexerComposite;
import io.micronaut.kubernetes.discovery.ServiceInstanceProviderInformerFactory;
import io.micronaut.kubernetes.util.KubernetesUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
 * @since 2.3
 */
@Singleton
public class KubernetesServiceInstanceEndpointProvider extends AbstractKubernetesServiceInstanceProvider {
    public static final String MODE = "endpoint";
    protected static final String RESOURCE_PLURAL = "endpoints";
    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceEndpointProvider.class);

    private final CoreV1ApiReactorClient client;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;

    private IndexerComposite<V1Endpoints> indexerComposite = null;

    /**
     * Creates kubernetes instance endpoint provider.
     *
     * @param client                                 client
     * @param discoveryConfiguration                 discovery configuration
     * @param serviceInstanceProviderInformerFactory service instance provider informer factory
     * @param watchEnabled                           flag whether to enable watch or fetch resources on request
     * @since 3.1
     */
    @Inject
    public KubernetesServiceInstanceEndpointProvider(CoreV1ApiReactorClient client,
                                                     KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration,
                                                     ServiceInstanceProviderInformerFactory serviceInstanceProviderInformerFactory,
                                                     @Value("${kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled:true}") boolean watchEnabled) {
        this.client = client;
        this.discoveryConfiguration = discoveryConfiguration;
        if (watchEnabled) {
            this.indexerComposite = serviceInstanceProviderInformerFactory.createInformersFor(this);
        }
    }

    /**
     * Creates kubernetes instance endpoint provider.
     *
     * @param client                 client
     * @param discoveryConfiguration discovery configuration
     * @deprecated use {@link KubernetesServiceInstanceEndpointProvider#KubernetesServiceInstanceEndpointProvider(CoreV1ApiReactorClient, KubernetesConfiguration.KubernetesDiscoveryConfiguration, ServiceInstanceProviderInformerFactory, boolean)}
     */
    public KubernetesServiceInstanceEndpointProvider(CoreV1ApiReactorClient client,
                                                     KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
        this(client, discoveryConfiguration, null, false);
    }

    @Override
    public String getMode() {
        return MODE;
    }

    @Override
    public Publisher<String> getServiceIds(String namespace) {
        Flux<V1Endpoints> endpointsFlux;
        if (indexerComposite != null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Fetching Endpoints from cache");
            }
            endpointsFlux = indexerComposite.getResources(namespace);
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Fetching Endpoints from API");
            }
            endpointsFlux = client.listNamespacedEndpoints(namespace, null, null, null, null, null, null, null, null, null)
                    .doOnError(ApiException.class, throwable -> LOG.error("Failed to list Endpoints from namespace [" + namespace + "]: " + throwable.getResponseBody(), throwable))
                    .flatMapIterable(V1EndpointsList::getItems);
        }

        return endpointsFlux
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

        if (indexerComposite != null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Fetching Endpoints from cache: {}", serviceConfiguration);
            }
            v1EndpointsMono = indexerComposite.getResource(serviceName, serviceNamespace);
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Fetching Endpoints from API: {}", serviceConfiguration);
            }
            v1EndpointsMono = client.readNamespacedEndpoints(serviceName, serviceNamespace, null, null, null)
                    .doOnError(ApiException.class, throwable -> LOG.error("Failed to list Endpoints [ " + serviceName + "] from namespace [" + serviceNamespace + "]: " + throwable.getResponseBody(), throwable));
        }

        return v1EndpointsMono
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

    @Override
    public Class<? extends KubernetesObject> getApiType() {
        return V1Endpoints.class;
    }

    @Override
    public Class<? extends KubernetesListObject> getApiListType() {
        return V1EndpointsList.class;
    }

    @Override
    public String getResorucePlural() {
        return RESOURCE_PLURAL;
    }
}
