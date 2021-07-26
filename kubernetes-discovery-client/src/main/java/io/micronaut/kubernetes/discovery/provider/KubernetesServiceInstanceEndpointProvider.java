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
import io.micronaut.kubernetes.client.v1.endpoints.Endpoints;
import io.micronaut.kubernetes.discovery.AbstractKubernetesServiceInstanceProvider;
import io.micronaut.kubernetes.util.KubernetesUtils;
import reactor.core.publisher.Flux;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
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
    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceEndpointProvider.class);

    private final KubernetesClient client;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;

    public KubernetesServiceInstanceEndpointProvider(KubernetesClient client,
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
                () -> new IllegalArgumentException("KubernetesServiceConfiguration is missing service name."));
        String serviceNamespace = serviceConfiguration.getNamespace().orElseThrow(
                () -> new IllegalArgumentException("KubernetesServiceConfiguration is missing namespace."));

        AtomicReference<Metadata> metadata = new AtomicReference<>();

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
            LOG.trace("Fetching Endpoints {}", serviceConfiguration);
        }

        return Flux.from(client.getEndpoints(serviceNamespace, serviceName))
                .doOnError(throwable -> {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Error while trying to list Endpoints {}", serviceConfiguration, throwable);
                    }
                })
                .filter(globalFilter)
                .doOnNext(endpoints -> metadata.set(endpoints.getMetadata()))
                .flatMapIterable(Endpoints::getSubsets)
                .filter(subset -> hasValidPortConfiguration(subset.getPorts(), serviceConfiguration))
                .filter(subset -> subset.getAddresses() != null && !subset.getAddresses().isEmpty())
                .map(subset -> subset
                        .getPorts()
                        .stream()
                        .filter(port -> !serviceConfiguration.getPort().isPresent() || port.getName().equals(serviceConfiguration.getPort().get()))
                        .flatMap(port -> subset.getAddresses().stream().map(address -> buildServiceInstance(serviceConfiguration.getServiceId(), port, address.getIp(), metadata.get())))
                        .collect(Collectors.toList()))
                .onErrorResume(throwable -> {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Error while processing discovered endpoints [" + serviceName + "]", throwable);
                    }
                    return Flux.just(Collections.emptyList());
                })
                .defaultIfEmpty(new ArrayList<>());
    }
}
