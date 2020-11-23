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

import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Pavol Gressa
 * @since 2.3
 */
@Singleton
public class KubernetesServiceInstanceServiceProvider extends AbstractKubernetesServiceInstanceProvider {
    public static final String MODE = "service";
    protected static final String EXTERNAL_NAME = "ExternalName";
    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceEndpointProvider.class);

    private final KubernetesClient client;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;

    public KubernetesServiceInstanceServiceProvider(KubernetesClient client, KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
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

        Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(discoveryConfiguration.getIncludes());
        Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(discoveryConfiguration.getExcludes());
        Predicate<KubernetesObject> labelsFilter = KubernetesUtils.getLabelsFilter(discoveryConfiguration.getLabels());

        if (LOG.isTraceEnabled()) {
            LOG.trace(String.format("Fetching service %s in namespace %s", serviceName, serviceNamespace));
        }

        return Flowable.fromPublisher(client.getService(serviceNamespace, serviceName))
                .doOnError(throwable -> LOG.error("Error while trying to list Kubernetes Services in the namespace [" + serviceNamespace + "]", throwable))
                .filter(compositePredicate(includesFilter, excludesFilter, labelsFilter))
                .doOnNext(service -> validatePortConfiguration(service.getSpec().getPorts(), serviceConfiguration))
                .map(service -> Stream.of(buildServiceInstance(serviceConfiguration, service))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .onErrorReturn(throwable -> {
                    LOG.error("Error while processing discovered service [" + serviceName + "]", throwable);
                    return new ArrayList<>();
                });
    }

    private ServiceInstance buildServiceInstance(KubernetesServiceConfiguration serviceConfiguration, Service service) throws URISyntaxException {
        if (service.getSpec().getClusterIp() != null) {
            return service.getSpec().getPorts().stream()
                    .filter(port -> !serviceConfiguration.getPort().isPresent() || port.getName().equals(serviceConfiguration.getPort().get()))
                    .map(port -> buildServiceInstance(serviceConfiguration.getServiceId(), port, service.getSpec().getClusterIp(), service.getMetadata()))
                    .findFirst().orElse(null);
        } else if (service.getSpec().getType().equals(EXTERNAL_NAME)) {
            return ServiceInstance
                    .builder(serviceConfiguration.getServiceId(), new URI(service.getSpec().getExternalName()))
                    .metadata(service.getMetadata().getLabels())
                    .build();
        } else {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to create service instance from service [" + serviceConfiguration.getServiceId() + "]");
            }
            return null;
        }
    }
}
