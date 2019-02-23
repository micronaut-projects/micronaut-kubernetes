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
import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.v1.Address;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.Port;
import io.micronaut.kubernetes.client.v1.endpoints.Endpoints;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link DiscoveryClient} implementation for Kubernetes using the API
 *
 * @author Alvaro Sanchez-Mariscal
 * @since 1.1
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
public class KubernetesDiscoveryClient implements DiscoveryClient {

    private final KubernetesClient client;

    @Inject
    public KubernetesDiscoveryClient(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(String serviceId) {
        //TODO parameterise namespace
        return Flowable.fromPublisher(client.getEndpoints("default", serviceId))
                .flatMapIterable(Endpoints::getSubsets)
                .map(subset -> subset
                        .getPorts()
                        .stream()
                        .flatMap(port -> subset.getAddresses().stream().map(address -> buildServiceInstance(serviceId, port, address)))
                        .collect(Collectors.toList()));
    }

    private ServiceInstance buildServiceInstance(String serviceId, Port port, Address address) {
        //TODO check for SSL
        return ServiceInstance.of(serviceId, address.getIp().getHostAddress(), port.getPort());
    }

    @Override
    public Publisher<List<String>> getServiceIds() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
