/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.kubernetes.client.openapi.discovery.provider;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.discovery.KubernetesServiceConfiguration;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.model.V1Endpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service instance provider uses Kubernetes Endpoints Informer as source of service discovery.
 */
@Context
@Requires(env = Environment.KUBERNETES)
@Requires(property = "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled", notEquals = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
final class KubernetesServiceInstanceEndpointInformerProvider extends AbstractV1EndpointsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceEndpointInformerProvider.class);

    private final SharedIndexInformerFactory sharedIndexInformerFactory;

    KubernetesServiceInstanceEndpointInformerProvider(KubernetesConfiguration kubernetesConfiguration,
                                                      SharedIndexInformerFactory sharedIndexInformerFactory,
                                                      List<KubernetesServiceConfiguration> serviceConfigurations) {
        super(kubernetesConfiguration.getDiscovery());
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;

        String mode = getMode();
        KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration = kubernetesConfiguration.getDiscovery();

        // Resolve namespaces for manually configured service discovery
        Set<String> namespaces = serviceConfigurations.stream()
            .filter(s -> s.getMode().map(ns -> ns.equalsIgnoreCase(mode)).orElse(mode.equalsIgnoreCase(discoveryConfiguration.getMode())))
            .filter(s -> s.getNamespace().isPresent())
            .map(s -> s.getNamespace().get())
            .collect(Collectors.toSet());

        // Add application namespace if the mode equals
        if (discoveryConfiguration.getMode().equalsIgnoreCase(mode)) {
            namespaces.add(kubernetesConfiguration.getNamespace());
        }

        LOG.debug("Going to create Informers of type {} in the namespaces: {}", V1Endpoints.class.getName(), namespaces);
        sharedIndexInformerFactory.sharedIndexInformersFor(V1Endpoints.class, new ArrayList<>(namespaces), null, 0);
    }

    @Override
    public Mono<V1Endpoints> getEndpoints(String name, String namespace) {
        LOG.trace("Using Informer to fetch endpoints [{}] from namespace [{}]", name, namespace);
        SharedIndexInformer<V1Endpoints> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(V1Endpoints.class, namespace);
        if (informer == null) {
            LOG.warn("Failed to find endpoints [{}] in namespace [{}] since Informer not found", name, namespace);
            return Mono.empty();
        }
        V1Endpoints endpoints = informer.getIndexer().getByKey(namespace + "/" + name);
        return Mono.justOrEmpty(endpoints);
    }

    @Override
    public Flux<V1Endpoints> listEndpoints(String namespace) {
        LOG.trace("Using Informer to fetch endpoints from namespace [{}]", namespace);
        SharedIndexInformer<V1Endpoints> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(V1Endpoints.class, namespace);
        if (informer == null) {
            LOG.warn("Failed to list endpoints from namespace [{}] since Informer not found", namespace);
            return Flux.empty();
        }
        return Flux.fromIterable(informer.getIndexer().list());
    }
}
