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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.v1.Address;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.Port;
import io.micronaut.kubernetes.client.v1.endpoints.Endpoints;
import io.micronaut.kubernetes.client.v1.services.ServiceList;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.micronaut.kubernetes.client.v1.KubernetesClient.SERVICE_ID;
import static io.micronaut.kubernetes.client.v1.KubernetesOperations.DEFAULT_NAMESPACE;

/**
 * A {@link DiscoveryClient} implementation for Kubernetes using the API.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
public class KubernetesDiscoveryClient implements DiscoveryClient {

    private final KubernetesClient client;

    /**
     *
     * @param client An HTTP Client to query the Kubernetes API.
     */
    public KubernetesDiscoveryClient(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(String serviceId) {
        if (SERVICE_ID.equals(serviceId)) {
            return Publishers.just(
                    Collections.singletonList(ServiceInstance.of(SERVICE_ID, URI.create("https://kubernetes")))
            );
        } else {
            //TODO parameterise namespace
            return Flowable.fromPublisher(client.getEndpoints(DEFAULT_NAMESPACE, serviceId))
                    .doOnError(Throwable::printStackTrace)
                    .flatMapIterable(Endpoints::getSubsets)
                    .map(subset -> subset
                            .getPorts()
                            .stream()
                            .flatMap(port -> subset.getAddresses().stream().map(address -> buildServiceInstance(serviceId, port, address)))
                            .collect(Collectors.toList()));
        }
    }

    private ServiceInstance buildServiceInstance(String serviceId, Port port, Address address) {
        //TODO check for SSL
        return ServiceInstance.of(serviceId, address.getIp().getHostAddress(), port.getPort());
    }

    /**
     *
     * @return A list of services metadata's name.
     */
    @Override
    public Publisher<List<String>> getServiceIds() {
        return Flowable.fromPublisher(client.listServices())
                .doOnError(Throwable::printStackTrace)
                .flatMapIterable(ServiceList::getItems)
                .map(service -> service.getMetadata().getName())
                .toList()
                .toFlowable();
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
