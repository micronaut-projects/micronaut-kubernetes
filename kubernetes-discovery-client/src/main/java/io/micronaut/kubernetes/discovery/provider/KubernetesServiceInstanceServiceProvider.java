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
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.micronaut.context.annotation.Value;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.kubernetes.discovery.KubernetesServiceConfiguration;
import io.micronaut.kubernetes.discovery.AbstractKubernetesServiceInstanceProvider;
import io.micronaut.kubernetes.discovery.IndexerComposite;
import io.micronaut.kubernetes.discovery.ServiceInstanceProviderInformerFactory;
import io.micronaut.kubernetes.util.KubernetesUtils;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service instance provider uses Kubernetes Service as source of service discovery.
 *
 * @author Pavol Gressa
 * @since 2.3
 */
@Singleton
public class KubernetesServiceInstanceServiceProvider extends AbstractKubernetesServiceInstanceProvider {
    public static final String MODE = "service";
    protected static final String RESOURCE_PLURAL = "services";
    protected static final String EXTERNAL_NAME = "ExternalName";
    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceServiceProvider.class);

    private final CoreV1ApiReactorClient client;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;

    private IndexerComposite<V1Service> indexerComposite = null;

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
    public KubernetesServiceInstanceServiceProvider(CoreV1ApiReactorClient client,
                                                    KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration,
                                                    ServiceInstanceProviderInformerFactory serviceInstanceProviderInformerFactory,
                                                    @Value("${kubernetes.client.discovery.mode-configuration.service.watch.enabled:true}") boolean watchEnabled) {
        this.client = client;
        this.discoveryConfiguration = discoveryConfiguration;
        if (watchEnabled) {
            this.indexerComposite = serviceInstanceProviderInformerFactory.createInformersFor(this);
        }
    }

    /**
     * Creates kubernetes instance service provider.
     *
     * @param client                 client
     * @param discoveryConfiguration discovery configuration
     * @deprecated use {@link KubernetesServiceInstanceServiceProvider#KubernetesServiceInstanceServiceProvider(CoreV1ApiReactorClient, KubernetesConfiguration.KubernetesDiscoveryConfiguration, ServiceInstanceProviderInformerFactory, boolean)}
     */
    public KubernetesServiceInstanceServiceProvider(CoreV1ApiReactorClient client,
                                                    KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
        this(client, discoveryConfiguration, null, false);
    }

    @Override
    public String getMode() {
        return MODE;
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(KubernetesServiceConfiguration serviceConfiguration) {
        String serviceName = serviceConfiguration.getName().orElseThrow(
                () -> new IllegalArgumentException("KubernetesServiceConfiguration is missing name."));
        String serviceNamespace = serviceConfiguration.getNamespace().orElseThrow(
                () -> new IllegalArgumentException("KubernetesServiceConfiguration is missing namespace."));

        Mono<V1Service> v1ServiceMono;
        if (indexerComposite != null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Fetching Service from cache: {}", serviceConfiguration);
            }
            v1ServiceMono = indexerComposite.getResource(serviceName, serviceNamespace);
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Fetching Service from API: {}", serviceConfiguration);
            }
            v1ServiceMono = client.readNamespacedService(serviceName, serviceNamespace, null, null, null)
                    .doOnError(ApiException.class, throwable -> LOG.error("Failed to read Service [" + serviceName + "] from namespace [" + serviceNamespace + "]: " + throwable.getResponseBody(), throwable));
        }

        return v1ServiceMono
                .filter(serviceConfigurationDiscoveryFilter(serviceConfiguration, discoveryConfiguration))
                .filter(service ->
                        hasValidPortConfiguration(
                                Optional.ofNullable(Objects.requireNonNull(service.getSpec()).getPorts())
                                        .orElse(new ArrayList<>())
                                        .stream()
                                        .map(PortBinder::fromServicePort)
                                        .collect(Collectors.toList()), serviceConfiguration))
                .map(service -> Stream.of(buildServiceInstance(serviceConfiguration, service))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .doOnError(throwable -> {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Error while processing discovered service [" + serviceName + "]", throwable);
                    }
                })
                .onErrorReturn(Collections.emptyList())
                .defaultIfEmpty(new ArrayList<>());
    }

    @Override
    public Publisher<String> getServiceIds(String namespace) {
        Flux<V1Service> serviceFlux;
        if (indexerComposite != null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Fetching Services from cache");
            }
            serviceFlux = indexerComposite.getResources(namespace);
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Fetching Services from API");
            }
            serviceFlux = client.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null)
                    .doOnError(ApiException.class, throwable -> LOG.error("Failed to list Services from namespace [" + namespace + "]: " + throwable.getResponseBody(), throwable))
                    .flatMapIterable(V1ServiceList::getItems);
        }

        return serviceFlux
                .filter(discoveryConfigurationFilter(discoveryConfiguration))
                .mapNotNull(KubernetesUtils::objectNameOrNull)
                .filter(Objects::nonNull);
    }

    private ServiceInstance buildServiceInstance(KubernetesServiceConfiguration serviceConfiguration, V1Service service) {
        final String clusterIp = Objects.requireNonNull(service.getSpec()).getClusterIP();
        if (clusterIp != null && !Objects.equals(clusterIp, "None")) {
            return Objects.requireNonNull(service.getSpec().getPorts()).stream()
                    .filter(port -> !serviceConfiguration.getPort().isPresent() || Objects.equals(port.getName(), serviceConfiguration.getPort().get()))
                    .map(port -> buildServiceInstance(serviceConfiguration.getServiceId(), PortBinder.fromServicePort(port), service.getSpec().getClusterIP(), service.getMetadata()))
                    .findFirst().orElse(null);

        } else if (Objects.equals(service.getSpec().getType(), EXTERNAL_NAME)) {
            final List<V1ServicePort> ports = service.getSpec().getPorts();

            V1ServicePort port = null;
            if (ports != null && !ports.isEmpty()) {
                port = ports.stream()
                        .filter(p -> !serviceConfiguration.getPort().isPresent() || Objects.equals(p.getName(), serviceConfiguration.getPort().get()))
                        .findFirst().orElse(null);
                if (port == null) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to assign ExternalName service [" + serviceConfiguration.getServiceId() +
                                "] configured port " + serviceConfiguration.getPort().get() + ", no such port in " +
                                "specification [" + ports.stream().map(V1ServicePort::getName).collect(Collectors.joining(",")) + "]");
                    }
                    return null;
                }
            }
            return buildServiceInstance(serviceConfiguration.getServiceId(), PortBinder.fromServicePort(port), service.getSpec().getExternalName(), service.getMetadata());
        } else {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to create service instance for [" + serviceConfiguration.getServiceId() + "]");
            }
            return null;
        }
    }

    @Override
    public Class<? extends KubernetesObject> getApiType() {
        return V1Service.class;
    }

    @Override
    public Class<? extends KubernetesListObject> getApiListType() {
        return V1ServiceList.class;
    }

    @Override
    public String getResorucePlural() {
        return RESOURCE_PLURAL;
    }
}
