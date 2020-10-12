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
import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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

    public KubernetesServiceInstanceEndpointProvider(KubernetesClient client, KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
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
        // TODO: manual configuration should take precendence over everything else, so no filtering
        Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(discoveryConfiguration.getIncludes());
        Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(discoveryConfiguration.getExcludes());
        Predicate<KubernetesObject> labelsFilter = KubernetesUtils.getLabelsFilter(discoveryConfiguration.getLabels());

        if (LOG.isTraceEnabled()) {
            LOG.trace(String.format("Fetching service %s endpoints in namespace %s", serviceName, serviceNamespace));
        }

        // TODO: fetch specific endpoint and do the label filteering afterwards
        return Flowable.fromPublisher(client.getEndpoints(serviceNamespace, serviceName))
                .doOnError(throwable -> LOG.error("Error while trying to list Kubernetes Endpoints [" + serviceName +
                        "] in the namespace [" + serviceNamespace + "]", throwable))
                // TODO: do we want to also filter manually configured service discovery
                .filter(compositePredicate(includesFilter, excludesFilter, labelsFilter))
                .doOnNext(endpoints -> metadata.set(endpoints.getMetadata()))
                .flatMapIterable(Endpoints::getSubsets)
                // TODO: discuss whether to raise exception totally or just log a message in case of missing port config mu .02 exception
                .doOnNext(subsets -> validatePortConfiguration(subsets.getPorts(), serviceConfiguration))
                .map(subset -> subset
                        .getPorts()
                        .stream()
                        .filter(port -> !serviceConfiguration.getPort().isPresent() || port.getName().equals(serviceConfiguration.getPort().get()))
                        .flatMap(port -> subset.getAddresses().stream().map(address -> buildServiceInstance(serviceConfiguration.getServiceId(), port, address.getIp(), metadata.get())))
                        .collect(Collectors.toList()))
                .onErrorReturn(throwable -> {
                    LOG.error("Error while processing discovered endpoints [" + serviceName + "]", throwable);
                    return new ArrayList<>();
                });
    }
}
