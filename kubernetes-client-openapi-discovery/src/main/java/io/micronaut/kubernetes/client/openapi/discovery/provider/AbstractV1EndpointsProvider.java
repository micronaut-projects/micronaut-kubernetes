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

import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.discovery.KubernetesServiceConfiguration;
import io.micronaut.kubernetes.client.openapi.discovery.KubernetesServiceInstanceProvider;
import io.micronaut.kubernetes.client.openapi.model.CoreV1EndpointPort;
import io.micronaut.kubernetes.client.openapi.model.V1EndpointAddress;
import io.micronaut.kubernetes.client.openapi.model.V1EndpointSubset;
import io.micronaut.kubernetes.client.openapi.model.V1Endpoints;
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

/**
 * Service instance provider which uses {@link V1Endpoints} as source of service discovery.
 */
abstract class AbstractV1EndpointsProvider implements KubernetesServiceInstanceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractV1EndpointsProvider.class);

    private static final String MODE = "endpoint";

    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;

    AbstractV1EndpointsProvider(KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
        this.discoveryConfiguration = discoveryConfiguration;
    }

    @Override
    public String getMode() {
        return MODE;
    }

    @Override
    public Publisher<String> getServiceIds(String namespace) {
        return listEndpoints(namespace)
            .filter(KubernetesDiscoveryUtils.discoveryConfigurationFilter(discoveryConfiguration))
            .map(endpoints -> endpoints.getMetadata().getName());
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(KubernetesServiceConfiguration serviceConfiguration) {
        String serviceName = serviceConfiguration.getName().orElseThrow(
            () -> new IllegalArgumentException("KubernetesServiceConfiguration is missing service name"));
        String serviceNamespace = serviceConfiguration.getNamespace().orElseThrow(
            () -> new IllegalArgumentException("KubernetesServiceConfiguration is missing namespace"));

        return getEndpoints(serviceName, serviceNamespace)
            .doOnNext(endpoints -> LOG.debug("Found [{}] endpoints. Applying filters (if any and service not manually configured)", endpoints.getMetadata().getName()))
            .filter(KubernetesDiscoveryUtils.serviceConfigurationDiscoveryFilter(serviceConfiguration, discoveryConfiguration))
            .map(endpoints -> buildServiceInstance(serviceConfiguration, endpoints))
            .doOnError(throwable -> LOG.error("Error while processing discovered Endpoints [{}]", serviceName, throwable))
            .onErrorReturn(Collections.emptyList())
            .defaultIfEmpty(Collections.emptyList());
    }

    private static List<ServiceInstance> buildServiceInstance(KubernetesServiceConfiguration serviceConfiguration, V1Endpoints endpoints) {
        String serviceName = serviceConfiguration.getName().get();
        List<V1EndpointSubset> endpointsSubsets = endpoints.getSubsets();
        if (CollectionUtils.isEmpty(endpointsSubsets)) {
            LOG.error("Failed to create a service instance for service [{}], 'subsets' not found in V1Endpoints: {}",
                serviceName,
                endpoints);
            return Collections.emptyList();
        }

        List<ServiceInstance> serviceInstances = new ArrayList<>();

        endpointsSubsets.forEach(endpointSubset -> {
            String errorMessage = validateEndpointsSubset(serviceConfiguration, endpointSubset);
            if (StringUtils.isNotEmpty(errorMessage)) {
                LOG.warn("Skipped processing of V1EndpointSubset for service [{}] - {}: V1EndpointSubset={}",
                    serviceName,
                    errorMessage,
                    endpointSubset);
                return;
            }
            Optional<CoreV1EndpointPort> endpointPortOpt = endpointSubset.getPorts().stream()
                .filter(port -> serviceConfiguration.getPort().isEmpty() || Objects.equals(port.getName(), serviceConfiguration.getPort().get()))
                .findFirst();
            if (endpointPortOpt.isEmpty()) {
                LOG.warn("Skipped processing of V1EndpointSubset for service [{}] - Configured port name [{}] doesn't match port names found in the 'ports' field: V1EndpointSubset={}",
                    serviceName,
                    serviceConfiguration.getPort().get(),
                    endpointSubset);
                return;
            }
            CoreV1EndpointPort endpointPort = endpointPortOpt.get();
            endpointSubset.getAddresses().forEach(endpointAddress -> {
                ServiceInstance serviceInstance = KubernetesDiscoveryUtils.buildServiceInstance(
                    serviceConfiguration.getServiceId(),
                    endpointPort.getName(),
                    endpointPort.getPort(),
                    endpointAddress.getIp(),
                    endpoints.getMetadata());
                serviceInstances.add(serviceInstance);
            });
        });
        return serviceInstances;
    }

    private static String validateEndpointsSubset(KubernetesServiceConfiguration serviceConfiguration, V1EndpointSubset endpointSubset) {
        List<V1EndpointAddress> endpointAddresses = endpointSubset.getAddresses();
        if (CollectionUtils.isEmpty(endpointAddresses)) {
            return "The 'addresses' field value not found";
        }
        List<CoreV1EndpointPort> endpointPorts = endpointSubset.getPorts();
        if (CollectionUtils.isEmpty(endpointPorts)) {
            return "The 'ports' field value not found";
        }
        if (endpointPorts.size() > 1 && serviceConfiguration.getPort().isEmpty()) {
            return "The 'ports' field contains multiple values, so desired port value needs to be configured manually";
        }
        return StringUtils.EMPTY_STRING;
    }

    abstract Mono<V1Endpoints> getEndpoints(String name, String namespace);

    abstract Flux<V1Endpoints> listEndpoints(String namespace);
}
