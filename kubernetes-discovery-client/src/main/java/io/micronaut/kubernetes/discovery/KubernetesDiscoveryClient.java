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
package io.micronaut.kubernetes.discovery;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.v1.*;
import io.micronaut.kubernetes.client.v1.endpoints.Endpoints;
import io.micronaut.kubernetes.client.v1.endpoints.EndpointsList;
import io.micronaut.kubernetes.client.v1.services.ServiceList;
import io.micronaut.kubernetes.util.KubernetesUtils;
import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.micronaut.kubernetes.client.v1.KubernetesClient.SERVICE_ID;

/**
 * A {@link DiscoveryClient} implementation for Kubernetes using the API.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(beans = {KubernetesClient.class, KubernetesConfiguration.KubernetesDiscoveryConfiguration.class})
@Requires(property = KubernetesConfiguration.KubernetesDiscoveryConfiguration.PREFIX + ".enabled", notEquals = StringUtils.FALSE)
@SuppressWarnings("WeakerAccess")
public class KubernetesDiscoveryClient implements DiscoveryClient {

    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesDiscoveryClient.class);

    private final KubernetesClient client;
    private final KubernetesConfiguration configuration;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;
    private final Map<String, KubernetesServiceConfiguration> serviceConfigurations;
    private final KubernetesServiceInstanceList instanceList;

    /**
     * @param client An HTTP Client to query the Kubernetes API.
     * @param configuration The configuration properties
     * @param discoveryConfiguration The discovery configuration properties
     * @param serviceConfigurations The manual service discovery configurations
     * @param instanceList The {@link KubernetesServiceInstanceList}
     */
    public KubernetesDiscoveryClient(KubernetesClient client,
                                     KubernetesConfiguration configuration,
                                     KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration,
                                     List<KubernetesServiceConfiguration> serviceConfigurations,
                                     KubernetesServiceInstanceList instanceList) {
        this.client = client;
        this.configuration = configuration;
        this.discoveryConfiguration = discoveryConfiguration;
        this.serviceConfigurations = serviceConfigurations.stream()
                .collect(Collectors.toMap(KubernetesServiceConfiguration::getServiceId, Function.identity()));
        this.instanceList = instanceList;
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(String serviceId) {
        if (!discoveryConfiguration.isEnabled()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Discovery configuration is not enabled");
            }
            return Publishers.just(Collections.emptyList());
        }
        if (SERVICE_ID.equals(serviceId)) {
            return Publishers.just(instanceList.getInstances());
        } else {
            KubernetesServiceConfiguration serviceConfiguration = serviceConfigurations.getOrDefault(
                    serviceId, new KubernetesServiceConfiguration(serviceId, null, configuration.getNamespace()));
            String serviceNamespace = serviceConfiguration.getNamespace().orElse(configuration.getNamespace());
            String serviceName = serviceConfiguration.getName().orElse(serviceId);
            String labelSelector = KubernetesUtils.computeLabelSelector(discoveryConfiguration.getLabels());
            AtomicReference<Metadata> metadata = new AtomicReference<>();
            Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(discoveryConfiguration.getIncludes());
            Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(discoveryConfiguration.getExcludes());

            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("Fetching for service %s endpoints in namespace %s", serviceName, serviceNamespace));
            }

            return Flowable.fromPublisher(client.listEndpoints(serviceNamespace, labelSelector))
                    .doOnError(throwable -> LOG.error("Error while trying to list Kubernetes Endpoints in the namespace [" + serviceNamespace + "]", throwable))
                    .flatMapIterable(EndpointsList::getItems)
                    .filter(endpoints -> endpoints.getMetadata().getName().equals(serviceName))
                    .filter(includesFilter)
                    .filter(excludesFilter)
                    .doOnNext(endpoints -> metadata.set(endpoints.getMetadata()))
                    .flatMapIterable(Endpoints::getSubsets)
                    .map(subset -> subset
                            .getPorts()
                            .stream()
                            .flatMap(port -> subset.getAddresses().stream().map(address -> buildServiceInstance(serviceId, port, address, metadata.get())))
                            .collect(Collectors.toList()))
                            .onErrorReturn(throwable -> new ArrayList<>());
        }
    }

    private ServiceInstance buildServiceInstance(String serviceId, Port port, Address address, Metadata metadata) {
        boolean isSecure = port.isSecure() || metadata.isSecure();
        String scheme = isSecure ? "https://" : "http://";
        URI uri = URI.create(scheme + address.getIp().getHostAddress() + ":" + port.getPort());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Building ServiceInstance for serviceId [{}] and URI [{}]", serviceId, uri.toString());
        }
        return ServiceInstance
                .builder(serviceId, uri)
                .metadata(metadata.getLabels())
                .build();
    }

    /**
     *
     * @return A list of services metadata's name.
     */
    @Override
    public Publisher<List<String>> getServiceIds() {
        String namespace = configuration.getNamespace();
        Map<String, String> labels = configuration.getDiscovery().getLabels();
        String labelSelector = KubernetesUtils.computeLabelSelector(labels);
        Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(discoveryConfiguration.getIncludes());
        Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(discoveryConfiguration.getExcludes());

        return Flowable.merge(
                Flowable.fromIterable(serviceConfigurations.keySet()),
                Flowable.fromPublisher(client.listServices(namespace, labelSelector))
                        .doOnError(throwable -> LOG.error("Error while trying to list all Kubernetes Services in the namespace [" + namespace + "]", throwable))
                        .flatMapIterable(ServiceList::getItems)
                        .filter(includesFilter)
                        .filter(excludesFilter)
                        .map(service -> service.getMetadata().getName())
        ).distinct().toList().toFlowable();
    }

    @Override
    public String getDescription() {
        return SERVICE_ID;
    }

    @Override
    public void close() throws IOException {
        //no op
    }
}
