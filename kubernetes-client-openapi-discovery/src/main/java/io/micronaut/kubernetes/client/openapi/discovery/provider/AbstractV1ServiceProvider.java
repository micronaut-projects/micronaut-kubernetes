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

    private static final String ERROR_MSG_PREFIX = "Failed to create a service instance for service";

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
            .map(service -> buildServiceInstance(serviceName, serviceConfiguration, service))
            .doOnError(throwable -> LOG.error("Error while processing discovered Service [{}]", serviceName, throwable))
            .onErrorReturn(Collections.emptyList())
            .defaultIfEmpty(Collections.emptyList());
    }

    private static List<ServiceInstance> buildServiceInstance(String serviceName, KubernetesServiceConfiguration serviceConfiguration, V1Service service) {
        V1ServiceSpec serviceSpec = service.getSpec();
        if (serviceSpec == null) {
            LOG.error("{} [{}] - The 'spec' field value not found: V1Service={}", ERROR_MSG_PREFIX, serviceName, service);
            return Collections.emptyList();
        }

        V1ServicePort servicePort = null;
        List<V1ServicePort> servicePorts = serviceSpec.getPorts();
        if (CollectionUtils.isNotEmpty(servicePorts)) {
            Optional<V1ServicePort> servicePortOpt = getServicePort(serviceName, serviceConfiguration, service);
            if (servicePortOpt.isEmpty()) {
                return Collections.emptyList();
            }
            servicePort = servicePortOpt.get();
        }

        String address;
        String clusterIp = serviceSpec.getClusterIP();
        if (clusterIp != null && !Objects.equals(clusterIp, "None")) {
            address = clusterIp;
        } else if (Objects.equals(serviceSpec.getType(), EXTERNAL_NAME)) {
            address = serviceSpec.getExternalName();
        } else {
            LOG.error("{} [{}] - Could not resolve address from V1Service: {}", ERROR_MSG_PREFIX, serviceName, service);
            return Collections.emptyList();
        }

        ServiceInstance serviceInstance = KubernetesDiscoveryUtils.buildServiceInstance(
            serviceConfiguration.getServiceId(),
            servicePort == null ? null : servicePort.getName(),
            servicePort == null ? null : servicePort.getPort(),
            address,
            service.getMetadata());
        LOG.trace("Created a service instance: {}", serviceInstance);
        return Collections.singletonList(serviceInstance);
    }

    private static Optional<V1ServicePort> getServicePort(String serviceName, KubernetesServiceConfiguration serviceConfiguration, V1Service service) {
        List<V1ServicePort> servicePorts = service.getSpec().getPorts();
        Optional<String> configPortNameOpt = serviceConfiguration.getPort();
        if (configPortNameOpt.isEmpty()) {
            if (servicePorts.size() > 1) {
                LOG.error("{} [{}] - The 'ports' field contains multiple values, so desired port value needs to be configured manually: V1Service={}",
                    ERROR_MSG_PREFIX, serviceName, service);
                return Optional.empty();
            }
            return Optional.of(servicePorts.get(0));
        }
        String configPortName = configPortNameOpt.get();
        Optional<V1ServicePort> servicePortOpt = servicePorts.stream()
            .filter(port -> Objects.equals(port.getName(), configPortName))
            .findFirst();
        if (servicePortOpt.isEmpty()) {
            LOG.error("{} [{}] - Configured port name [{}] doesn't match port names found in the 'ports' field: V1Service={}",
                ERROR_MSG_PREFIX, serviceName, configPortName, service);
        }
        return servicePortOpt;
    }

    abstract Mono<V1Service> getService(String name, String namespace);

    abstract Flux<V1Service> listServices(String namespace);
}
