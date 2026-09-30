package io.micronaut.docs.informer

import io.kubernetes.client.informer.SharedIndexInformer
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.kubernetes.client.informer.SharedIndexInformerFactory

class SharedIndexInformerFactoryExample {

    private final SharedIndexInformerFactory factory

    SharedIndexInformerFactoryExample(SharedIndexInformerFactory factory) {
        this.factory = factory
    }

    SharedIndexInformer<V1ConfigMap> createInformer() {
        //tag::create[]
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = factory.sharedIndexInformerFor(
                V1ConfigMap, // <1>
                V1ConfigMapList, // <2>
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

    SharedIndexInformer<V1ConfigMap> getInformer(String namespace) {
        //tag::get[]
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = factory.getExistingSharedIndexInformer(
                "default", // <1>
                V1ConfigMap) // <2>
        //end::get[]
        return sharedIndexInformer
    }
}
