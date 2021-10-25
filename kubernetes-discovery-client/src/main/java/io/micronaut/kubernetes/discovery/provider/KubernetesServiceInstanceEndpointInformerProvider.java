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

import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1EndpointsList;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.discovery.informer.IndexerComposite;
import io.micronaut.kubernetes.discovery.informer.IndexerCompositeFactory;
import io.micronaut.kubernetes.discovery.informer.InstanceProviderInformerNamespaceResolver;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service instance provider uses Kubernetes Endpoints Informer as source of service discovery.
 *
 * @author Pavol Gressa
 * @since 3.2
 */
@Requires(property = "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled", notEquals = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
@Singleton
public class KubernetesServiceInstanceEndpointInformerProvider extends AbstractV1EndpointsProvider {
    protected static final String RESOURCE_PLURAL = "endpoints";
    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceEndpointInformerProvider.class);

    private IndexerComposite<V1Endpoints> indexerComposite;

    /**
     * Creates kubernetes instance endpoint provider.
     *
     * @param discoveryConfiguration    discovery configuration
     * @param indexerCompositeFactory   service instance provider informer factory
     * @param informerNamespaceResolver namespace resolver
     */
    public KubernetesServiceInstanceEndpointInformerProvider(KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration,
                                                             InstanceProviderInformerNamespaceResolver informerNamespaceResolver,
                                                             IndexerCompositeFactory indexerCompositeFactory) {
        super(discoveryConfiguration);
        this.indexerComposite = indexerCompositeFactory.createInformersFor(
                V1Endpoints.class,
                V1EndpointsList.class,
                RESOURCE_PLURAL,
                informerNamespaceResolver.resolveInformerNamespaces(this));
    }

    @Override
    public Mono<V1Endpoints> getEndpoints(String name, String namespace) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Using Indexer to fetch Endpoints[{}] from namespace [{}]", name, namespace);
        }

        return indexerComposite.getResource(name, namespace);
    }

    @Override
    public Flux<V1Endpoints> listEndpoints(String namespace) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Using Indexer to fetch endpoints from namespace [{}]", namespace);
        }

        return indexerComposite.getResources(namespace);
    }
}
