package io.micronaut.docs.informer

import io.kubernetes.client.informer.SharedIndexInformer
import io.kubernetes.client.informer.cache.Indexer
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.informer.SharedIndexInformerFactory
import jakarta.inject.Singleton

@Requires(property = "spec.name", value = "ConfigMapInformerSpec")
//tag::cache[]
@Singleton
class SharedInformerCache {

    private final SharedIndexInformerFactory sharedIndexInformerFactory

    SharedInformerCache(SharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory
    }

    /**
     * Get all config maps from informer from namespace.
     */
    List<V1ConfigMap> getConfigMaps(String namespace) {
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = sharedIndexInformerFactory.getExistingSharedIndexInformer(namespace, V1ConfigMap)
        if (sharedIndexInformer != null) {
            Indexer<V1ConfigMap> indexer = sharedIndexInformer.indexer
            return indexer.list()
        } else {
            return null
        }
    }
}
//end::cache[]
