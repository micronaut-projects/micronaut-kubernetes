/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.discovery.informer;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.cache.Indexer;
import io.micronaut.core.annotation.Internal;
import io.micronaut.kubernetes.client.informer.SharedIndexInformerFactory;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Factory for {@link IndexerComposite}.
 *
 * @author Pavol Gressa
 * @since 3.2
 */
@Internal
@Singleton
public class IndexerCompositeFactory {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerCompositeFactory.class);

    private final SharedIndexInformerFactory sharedIndexInformerFactory;

    public IndexerCompositeFactory(SharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
    }

    /**
     * Creates the {@link IndexerComposite} that is populated by generated {@link SharedIndexInformer}'s {@link Indexer}.
     *
     * @param apiType        informer type
     * @param apiListType    informer list type
     * @param resourcePlural informer plural
     * @param namespaces     namespaces to create {@link SharedIndexInformer}s
     * @param <ApiType>      type of composite
     * @return indexer composite
     */
    @SuppressWarnings("unchecked")
    public <ApiType extends KubernetesObject> IndexerComposite<ApiType> createInformersFor(
            Class<ApiType> apiType,
            Class<? extends KubernetesListObject> apiListType,
            String resourcePlural,
            Set<String> namespaces) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Going to create Informers of type {} in the namespaces: {}", apiType, namespaces);
        }

        IndexerComposite<ApiType> indexerComposite = new IndexerComposite<>();
        for (String namespace : namespaces) {
            SharedIndexInformer<? extends KubernetesObject> informer = sharedIndexInformerFactory.sharedIndexInformerFor(
                    apiType,
                    apiListType,
                    resourcePlural,
                    "",
                    namespace,
                    null,
                    null,
                    true);
            indexerComposite.add(namespace, (Indexer<ApiType>) informer.getIndexer());
        }

        return indexerComposite;
    }
}
