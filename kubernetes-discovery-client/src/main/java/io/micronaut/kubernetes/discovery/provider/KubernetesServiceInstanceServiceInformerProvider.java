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

import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
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
 * Service instance provider uses Kubernetes Service Informer as source of service discovery.
 *
 * @author Pavol Gressa
 * @since 3.2
 */
@Requires(property = "kubernetes.client.discovery.mode-configuration.service.watch.enabled", notEquals = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
@Singleton
public class KubernetesServiceInstanceServiceInformerProvider extends AbstractV1ServiceProvider {
    protected static final String RESOURCE_PLURAL = "services";
    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceServiceInformerProvider.class);

    private final IndexerComposite<V1Service> indexerComposite;

    /**
     * Creates kubernetes instance endpoint provider.
     *
     * @param discoveryConfiguration    discovery configuration
     * @param indexerCompositeFactory   service instance provider informer factory
     * @param informerNamespaceResolver namespace resolver
     */
    public KubernetesServiceInstanceServiceInformerProvider(KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration,
                                                            InstanceProviderInformerNamespaceResolver informerNamespaceResolver,
                                                            IndexerCompositeFactory indexerCompositeFactory) {

        super(discoveryConfiguration);
        this.indexerComposite = indexerCompositeFactory.createInformersFor(
                V1Service.class,
                V1ServiceList.class,
                RESOURCE_PLURAL,
                informerNamespaceResolver.resolveInformerNamespaces(this));

    }

    @Override
    public Mono<V1Service> getService(String name, String namespace) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Using Indexer to fetch Service[{}] from namespace [{}]", name, namespace);
        }

        return indexerComposite.getResource(name, namespace);
    }

    @Override
    public Flux<V1Service> listServices(String namespace) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Using Indexer to fetch services from namespace [{}]", namespace);
        }

        return indexerComposite.getResources(namespace);
    }
}
