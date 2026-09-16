package io.micronaut.docs.informer

import io.kubernetes.client.informer.SharedIndexInformer
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.kubernetes.client.informer.SharedIndexInformerFactory

class SharedIndexInformerFactoryExample(private val factory: SharedIndexInformerFactory) {

    fun createInformer(): SharedIndexInformer<V1ConfigMap> {
        //tag::create[]
        val sharedIndexInformer = factory.sharedIndexInformerFor(
                V1ConfigMap::class.java, // <1>
                V1ConfigMapList::class.java, // <2>
                "configmaps", // <3>
                "",  // <4>
                "default",  // <5>
                null,
                null,
                true
        )
        //end::create[]
        return sharedIndexInformer
    }

    fun getInformer(namespace: String): SharedIndexInformer<V1ConfigMap>? {
        //tag::get[]
        val sharedIndexInformer = factory.getExistingSharedIndexInformer(
                "default", // <1>
                V1ConfigMap::class.java) // <2>
        //end::get[]
        return sharedIndexInformer
    }
}
