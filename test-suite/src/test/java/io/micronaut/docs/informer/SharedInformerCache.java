package io.micronaut.docs.informer;

import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.micronaut.context.annotation.Requires;
import io.micronaut.kubernetes.client.informer.SharedIndexInformerFactory;
import jakarta.inject.Singleton;

import java.util.List;

@Requires(property = "spec.name", value = "ConfigMapInformerSpec")
//tag::cache[]
@Singleton
public class SharedInformerCache {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;

    public SharedInformerCache(SharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
    }

    /**
     * Get all config maps from informer from namespace.
     */
    List<V1ConfigMap> getConfigMaps(String namespace) {
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = sharedIndexInformerFactory.getExistingSharedIndexInformer(namespace, V1ConfigMap.class);
        if (sharedIndexInformer != null) {
            Indexer<V1ConfigMap> indexer = sharedIndexInformer.getIndexer();
            return indexer.list();
        } else {
            return null;
        }
    }
}
//end::cache[]
