/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.kubernetes.discovery;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.v1.*;
import io.micronaut.kubernetes.client.v1.endpoints.Endpoints;
import io.micronaut.kubernetes.client.v1.services.ServiceList;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
@Requires(beans = {KubernetesClient.class, KubernetesDiscoveryConfiguration.class})
@Requires(property = KubernetesDiscoveryConfiguration.PREFIX + ".enabled", notEquals = StringUtils.FALSE)
@Replaces(bean = io.micronaut.discovery.kubernetes.KubernetesDiscoveryClient.class)
@SuppressWarnings("WeakerAccess")
public class KubernetesDiscoveryClient implements DiscoveryClient {

    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesDiscoveryClient.class);

    private final KubernetesClient client;
    private final KubernetesConfiguration configuration;
    private final KubernetesDiscoveryConfiguration discoveryConfiguration;
    private final KubernetesServiceInstanceList instanceList;

    /**
     * @param client An HTTP Client to query the Kubernetes API.
     * @param configuration The configuration properties
     * @param discoveryConfiguration The discovery configuration properties
     * @param instanceList The {@link KubernetesServiceInstanceList}
     */
    public KubernetesDiscoveryClient(KubernetesClient client,
                                     KubernetesConfiguration configuration,
                                     KubernetesDiscoveryConfiguration discoveryConfiguration,
                                     KubernetesServiceInstanceList instanceList) {
        this.client = client;
        this.configuration = configuration;
        this.discoveryConfiguration = discoveryConfiguration;
        this.instanceList = instanceList;
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(String serviceId) {
        if (!discoveryConfiguration.isEnabled()) {
            LOG.debug("Discovery configuration is not enabled");
            return Publishers.just(Collections.emptyList());
        }
        if (SERVICE_ID.equals(serviceId)) {
            return Publishers.just(instanceList.getInstances());
        } else {
            AtomicReference<Metadata> metadata = new AtomicReference<>();
            String namespace = configuration.getNamespace();
            return Flowable.fromPublisher(client.getEndpoints(namespace, serviceId))
                    .doOnError(throwable -> LOG.error("Error while trying to get Kubernetes Endpoints for the service [" + serviceId + "] in the namespace [" + namespace + "]", throwable))
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
        return Flowable.fromPublisher(client.listServices(namespace))
                .doOnError(throwable -> LOG.error("Error while trying to list all Kubernetes Services in the namespace [" + namespace + "]", throwable))
                .flatMapIterable(ServiceList::getItems)
                .map(service -> service.getMetadata().getName())
                .toList()
                .toFlowable();
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
