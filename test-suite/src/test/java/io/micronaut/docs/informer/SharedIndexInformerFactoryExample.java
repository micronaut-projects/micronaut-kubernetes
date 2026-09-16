package io.micronaut.docs.informer;

import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.micronaut.kubernetes.client.informer.SharedIndexInformerFactory;

public class SharedIndexInformerFactoryExample {

    private final SharedIndexInformerFactory factory;

    public SharedIndexInformerFactoryExample(SharedIndexInformerFactory factory) {
        this.factory = factory;
    }

    public SharedIndexInformer<V1ConfigMap> createInformer() {
        //tag::create[]
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = factory.sharedIndexInformerFor(
                V1ConfigMap.class, // <1>
                V1ConfigMapList.class, // <2>
                "configmaps", // <3>
                "",  // <4>
                "default",  // <5>
                null,
                null,
                true
        );
        //end::create[]
        return sharedIndexInformer;
    }

    public SharedIndexInformer<V1ConfigMap> getInformer(String namespace) {
        //tag::get[]
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = factory.getExistingSharedIndexInformer(
                "default", // <1>
                V1ConfigMap.class); // <2>
        //end::get[]
        return sharedIndexInformer;
    }
}
