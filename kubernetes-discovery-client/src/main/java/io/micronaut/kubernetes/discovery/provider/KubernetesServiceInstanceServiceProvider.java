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

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.rxjava2.CoreV1ApiRxClient;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.discovery.KubernetesServiceConfiguration;
import io.micronaut.kubernetes.discovery.AbstractKubernetesServiceInstanceProvider;
import io.micronaut.kubernetes.util.KubernetesUtils;
import io.reactivex.functions.Predicate;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
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
    protected static final String EXTERNAL_NAME = "ExternalName";
    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceServiceProvider.class);

    private final CoreV1ApiRxClient client;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;

    public KubernetesServiceInstanceServiceProvider(CoreV1ApiRxClient client,
                                                    KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
        this.client = client;
        this.discoveryConfiguration = discoveryConfiguration;
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

        Predicate<KubernetesObject> globalFilter;
        if (!serviceConfiguration.isManual()) {
            globalFilter = compositePredicate(
                    KubernetesUtils.getIncludesFilter(discoveryConfiguration.getIncludes()),
                    KubernetesUtils.getExcludesFilter(discoveryConfiguration.getExcludes()),
                    KubernetesUtils.getLabelsFilter(discoveryConfiguration.getLabels())
            );
        } else {
            globalFilter = f -> true;
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Fetching Service {}", serviceConfiguration);
        }

        return client.readNamespacedServiceAsync(serviceName, serviceNamespace, null, null, null)
                .toFlowable()
                .doOnError(throwable -> {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Error while trying to get Service {}", serviceConfiguration, throwable);
                    }
                })
                .filter(globalFilter)
                .filter(service ->
                        hasValidPortConfiguration(Optional.ofNullable(service.getSpec().getPorts()).orElse(new ArrayList<>()).stream().map(PortBinder::fromServicePort).collect(Collectors.toList()), serviceConfiguration))
                .map(service -> Stream.of(buildServiceInstance(serviceConfiguration, service))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .onErrorReturn(throwable -> {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Error while processing discovered service [" + serviceName + "]", throwable);
                    }
                    return Flux.just(Collections.emptyList());
                })
                .defaultIfEmpty(new ArrayList<>());
    }

    private List<ServiceInstance> buildServiceInstanceList(KubernetesServiceConfiguration serviceConfiguration, Service service) {
        try {
            return Stream.of(buildServiceInstance(serviceConfiguration, service))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (UnknownHostException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("UnknownHostException building Service Instance list");
            }
            return Collections.emptyList();
        }
    }

    private ServiceInstance buildServiceInstance(KubernetesServiceConfiguration serviceConfiguration, V1Service service) {

        if (service.getSpec().getClusterIP() != null) {
            return service.getSpec().getPorts().stream()
                    .filter(port -> !serviceConfiguration.getPort().isPresent() || port.getName().equals(serviceConfiguration.getPort().get()))
                    .map(port -> buildServiceInstance(serviceConfiguration.getServiceId(), PortBinder.fromServicePort(port), service.getSpec().getClusterIP(), service.getMetadata()))
                    .findFirst().orElse(null);

        } else if (service.getSpec().getType().equals(EXTERNAL_NAME)) {
            final List<V1ServicePort> ports = service.getSpec().getPorts();

            V1ServicePort port = null;
            if (ports != null && !ports.isEmpty()) {
                port = ports.stream()
                        .filter(p -> !serviceConfiguration.getPort().isPresent() || p.getName().equals(serviceConfiguration.getPort().get()))
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
}
