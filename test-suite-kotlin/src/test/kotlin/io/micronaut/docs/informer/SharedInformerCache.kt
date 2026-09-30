package io.micronaut.docs.informer

import io.kubernetes.client.openapi.models.V1ConfigMap
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.informer.SharedIndexInformerFactory
import jakarta.inject.Singleton

@Requires(property = "spec.name", value = "ConfigMapInformerSpec")
//tag::cache[]
@Singleton
class SharedInformerCache(private val sharedIndexInformerFactory: SharedIndexInformerFactory) {

    /**
     * Get all config maps from informer from namespace.
     */
    fun getConfigMaps(namespace: String): List<V1ConfigMap>? {
        val sharedIndexInformer = sharedIndexInformerFactory.getExistingSharedIndexInformer(namespace, V1ConfigMap::class.java)
        return if (sharedIndexInformer != null) {
            val indexer = sharedIndexInformer.indexer
            indexer.list()
        } else {
            null
        }
    }
}
//end::cache[]
