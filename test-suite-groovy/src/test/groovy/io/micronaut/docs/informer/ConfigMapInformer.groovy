package io.micronaut.docs.informer

import io.kubernetes.client.informer.ResourceEventHandler
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.informer.Informer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Requires(property = "spec.name", value = "ConfigMapInformerSpec")
//tag::handler[]
@Informer(apiType = V1ConfigMap, apiListType = V1ConfigMapList) // <1>
class ConfigMapInformer implements ResourceEventHandler<V1ConfigMap> { // <2>

    //end::handler[]
    static final Logger LOG = LoggerFactory.getLogger(ConfigMapInformer)

    final List<V1ConfigMap> added = []
    final List<V1ConfigMap> updated = []
    final List<V1ConfigMap> deleted = []

    //tag::handler[]
    @Override
    void onAdd(V1ConfigMap obj) {
        //end::handler[]
        LOG.info("ADDED CONFIG MAP: {}", obj)
        added.add(obj)
        //tag::handler[]
    }

    @Override
    void onUpdate(V1ConfigMap oldObj, V1ConfigMap newObj) {
        //end::handler[]
        LOG.info("UPDATED CONFIG MAP: {}", newObj)
        updated.add(newObj)
        //tag::handler[]
    }

    @Override
    void onDelete(V1ConfigMap obj, boolean deletedFinalStateUnknown) {
        //end::handler[]
        LOG.info("DELETED CONFIG MAP: {}", obj)
        deleted.add(obj)
        //tag::handler[]
    }
}
//end::handler[]
