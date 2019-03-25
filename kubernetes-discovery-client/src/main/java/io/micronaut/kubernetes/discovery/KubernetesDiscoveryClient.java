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
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.v1.Address;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.Metadata;
import io.micronaut.kubernetes.client.v1.Port;
import io.micronaut.kubernetes.client.v1.endpoints.Endpoints;
import io.micronaut.kubernetes.client.v1.services.ServiceList;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
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
public class KubernetesDiscoveryClient implements DiscoveryClient {

    public static final String KUBERNETES_URI = "https://kubernetes";
    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesDiscoveryClient.class);

    private final KubernetesClient client;
    private final KubernetesDiscoveryConfiguration configuration;

    /**
     * @param client An HTTP Client to query the Kubernetes API.
     * @param configuration The configuration properties
     */
    public KubernetesDiscoveryClient(KubernetesClient client,
                                     KubernetesDiscoveryConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(String serviceId) {
        if (SERVICE_ID.equals(serviceId)) {
            return Publishers.just(Collections.singletonList(ServiceInstance.of(SERVICE_ID, URI.create(KUBERNETES_URI))));
        } else {
            AtomicReference<Metadata> metadata = new AtomicReference<>();
            String namespace = configuration.getKubernetesConfiguration().getNamespace();
            return Flowable.fromPublisher(client.getEndpoints(namespace, serviceId))
                    .doOnError(throwable -> LOG.error("Error while trying to get Kubernetes Endpoints for the service=[" + serviceId + "] in the namespace=[" + namespace + "]", throwable))
                    .doOnNext(endpoints -> metadata.set(endpoints.getMetadata()))
                    .flatMapIterable(Endpoints::getSubsets)
                    .map(subset -> subset
                            .getPorts()
                            .stream()
                            .flatMap(port -> subset.getAddresses().stream().map(address -> buildServiceInstance(serviceId, port, address, metadata.get())))
                            .collect(Collectors.toList()));
        }
    }

    private ServiceInstance buildServiceInstance(String serviceId, Port port, Address address, Metadata metadata) {
        boolean isSecure = port.isSecure() || metadata.isSecure();
        String scheme = isSecure ? "https://" : "http://";
        URI uri = URI.create(scheme + address.getIp().getHostAddress() + ":" + port.getPort());
        if (LOG.isTraceEnabled()) {
            LOG.trace("ServiceInstance for serviceId={} has URI={}", serviceId, uri.toString());
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
        return Flowable.fromPublisher(client.listServices())
                .doOnError(throwable -> LOG.error("Error while trying to list all Kubernetes Services", throwable))
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
