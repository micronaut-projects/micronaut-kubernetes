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
import io.micronaut.kubernetes.client.openapi.model.V1Service;
import io.micronaut.kubernetes.client.openapi.model.V1ServicePort;
import io.micronaut.kubernetes.client.openapi.model.V1ServiceSpec;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service instance provider which uses {@link V1Service} as source of service discovery.
 */
abstract class AbstractV1ServiceProvider implements KubernetesServiceInstanceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractV1ServiceProvider.class);

    private static final String MODE = "service";
    private static final String EXTERNAL_NAME = "ExternalName";

    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;

    AbstractV1ServiceProvider(KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
        this.discoveryConfiguration = discoveryConfiguration;
    }

    @Override
    public String getMode() {
        return MODE;
    }

    @Override
    public Publisher<String> getServiceIds(String namespace) {
        return listServices(namespace)
            .filter(KubernetesDiscoveryUtils.discoveryConfigurationFilter(discoveryConfiguration))
            .map(service -> service.getMetadata().getName());
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(KubernetesServiceConfiguration serviceConfiguration) {
        String serviceName = serviceConfiguration.getName().orElseThrow(
            () -> new IllegalArgumentException("KubernetesServiceConfiguration is missing name."));
        String serviceNamespace = serviceConfiguration.getNamespace().orElseThrow(
            () -> new IllegalArgumentException("KubernetesServiceConfiguration is missing namespace."));

        return getService(serviceName, serviceNamespace)
            .doOnNext(endpoints -> LOG.debug("Found [{}] service. Applying filters (if any and service not manually configured)", endpoints.getMetadata().getName()))
            .filter(KubernetesDiscoveryUtils.serviceConfigurationDiscoveryFilter(serviceConfiguration, discoveryConfiguration))
            .map(service -> buildServiceInstance(serviceConfiguration, service))
            .doOnError(throwable -> LOG.error("Error while processing discovered Service [{}]", serviceConfiguration.getName(), throwable))
            .onErrorReturn(Collections.emptyList())
            .defaultIfEmpty(Collections.emptyList());
    }

    private static List<ServiceInstance> buildServiceInstance(KubernetesServiceConfiguration serviceConfiguration, V1Service service) {
        String errorMessage = validateService(serviceConfiguration, service);
        if (StringUtils.isNotEmpty(errorMessage)) {
            LOG.error("Failed to create a service instance for service [{}] - {}: V1Service={}",
                serviceConfiguration.getName(),
                errorMessage,
                service);
            return Collections.emptyList();
        }

        V1ServiceSpec serviceSpec = service.getSpec();
        Optional<V1ServicePort> servicePortOpt = serviceSpec.getPorts().stream()
            .filter(port -> serviceConfiguration.getPort().isEmpty() || Objects.equals(port.getName(), serviceConfiguration.getPort().get()))
            .findFirst();
        if (servicePortOpt.isEmpty()) {
            LOG.error("Failed to create a service instance for service [{}] - Configured port name [{}] doesn't match port names found in the 'ports' field: V1Service={}",
                serviceConfiguration.getName(),
                serviceConfiguration.getPort().get(),
                service);
            return Collections.emptyList();
        }

        String address;
        String clusterIp = serviceSpec.getClusterIP();
        if (clusterIp != null && !Objects.equals(clusterIp, "None")) {
            address = serviceSpec.getClusterIP();
        } else if (Objects.equals(serviceSpec.getType(), EXTERNAL_NAME)) {
            address = serviceSpec.getExternalName();
        } else {
            LOG.error("Failed to create a service instance for service [{}], could not resolve service address from V1Service: {}",
                serviceConfiguration.getName(),
                service);
            return Collections.emptyList();
        }

        V1ServicePort servicePort = servicePortOpt.get();
        ServiceInstance serviceInstance = KubernetesDiscoveryUtils.buildServiceInstance(
            serviceConfiguration.getServiceId(),
            servicePort.getName(),
            servicePort.getPort(),
            address,
            service.getMetadata());
        return Collections.singletonList(serviceInstance);
    }

    private static String validateService(KubernetesServiceConfiguration serviceConfiguration, V1Service service) {
        V1ServiceSpec serviceSpec = service.getSpec();
        if (serviceSpec == null) {
            return "The 'spec' field value not found";
        }
        List<V1ServicePort> servicePorts = serviceSpec.getPorts();
        if (CollectionUtils.isEmpty(servicePorts)) {
            return "The 'ports' field value not found";
        }
        if (servicePorts.size() > 1 && serviceConfiguration.getPort().isEmpty()) {
            return "The 'ports' field contains multiple values, so desired port value needs to be configured manually";
        }
        return StringUtils.EMPTY_STRING;
    }

    abstract Mono<V1Service> getService(String name, String namespace);

    abstract Flux<V1Service> listServices(String namespace);
}
