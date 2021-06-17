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

import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.v1.*;
import io.micronaut.kubernetes.client.v1.services.Service;
import io.micronaut.kubernetes.discovery.AbstractKubernetesServiceInstanceProvider;
import io.micronaut.kubernetes.util.KubernetesUtils;
import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    private final KubernetesClient client;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;

    public KubernetesServiceInstanceServiceProvider(KubernetesClient client,
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

        return Flowable.fromPublisher(client.getService(serviceNamespace, serviceName))
                .doOnError(throwable -> {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Error while trying to get Service {}", serviceConfiguration, throwable);
                    }
                })
                .filter(globalFilter)
                .filter(service -> hasValidPortConfiguration(service.getSpec().getPorts(), serviceConfiguration))
                .map(service -> Stream.of(buildServiceInstance(serviceConfiguration, service))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .onErrorReturn(throwable -> {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Error while processing discovered service [" + serviceName + "]", throwable);
                    }
                    return new ArrayList<>();
                })
                .defaultIfEmpty(new ArrayList<>());
    }

    private ServiceInstance buildServiceInstance(KubernetesServiceConfiguration serviceConfiguration, Service service)
            throws UnknownHostException {

        if (service.getSpec().getClusterIp() != null) {
            return service.getSpec().getPorts().stream()
                    .filter(port -> !serviceConfiguration.getPort().isPresent() || port.getName().equals(serviceConfiguration.getPort().get()))
                    .map(port -> buildServiceInstance(serviceConfiguration.getServiceId(), port, service.getSpec().getClusterIp(), service.getMetadata()))
                    .findFirst().orElse(null);

        } else if (service.getSpec().getType().equals(EXTERNAL_NAME)) {
            final List<Port> ports = service.getSpec().getPorts();
            final InetAddress inetAddress = InetAddress.getByName(service.getSpec().getExternalName());

            Port port = null;
            if (ports != null && !ports.isEmpty()) {
                port = ports.stream()
                        .filter(p -> !serviceConfiguration.getPort().isPresent() || p.getName().equals(serviceConfiguration.getPort().get()))
                        .findFirst().orElse(null);
                if (port == null) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to assign ExternalName service [" + serviceConfiguration.getServiceId() +
                                "] configured port " + serviceConfiguration.getPort().get() + ", no such port in " +
                                "specification [" + ports.stream().map(Port::getName).collect(Collectors.joining(",")) + "]");
                    }
                    return null;
                }
            }

            return buildServiceInstance(serviceConfiguration.getServiceId(), port, inetAddress, service.getMetadata());
        } else {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to create service instance for [" + serviceConfiguration.getServiceId() + "]");
            }
            return null;
        }
    }
}
