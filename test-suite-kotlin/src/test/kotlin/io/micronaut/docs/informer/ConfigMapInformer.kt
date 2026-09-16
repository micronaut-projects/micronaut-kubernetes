package io.micronaut.docs.informer

import io.kubernetes.client.informer.ResourceEventHandler
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.informer.Informer
import org.slf4j.LoggerFactory

@Requires(property = "spec.name", value = "ConfigMapInformerSpec")
//tag::handler[]
@Informer(apiType = V1ConfigMap::class, apiListType = V1ConfigMapList::class) // <1>
class ConfigMapInformer : ResourceEventHandler<V1ConfigMap> { // <2>

    //end::handler[]
    companion object {
        val LOG = LoggerFactory.getLogger(ConfigMapInformer::class.java)
    }

    val added: MutableList<V1ConfigMap> = ArrayList()
    val updated: MutableList<V1ConfigMap> = ArrayList()
    val deleted: MutableList<V1ConfigMap> = ArrayList()

    //tag::handler[]
    override fun onAdd(obj: V1ConfigMap) {
        //end::handler[]
        LOG.info("ADDED CONFIG MAP: {}", obj)
        added.add(obj)
        //tag::handler[]
    }

    override fun onUpdate(oldObj: V1ConfigMap, newObj: V1ConfigMap) {
        //end::handler[]
        LOG.info("UPDATED CONFIG MAP: {}", newObj)
        updated.add(newObj)
        //tag::handler[]
    }

    override fun onDelete(obj: V1ConfigMap, deletedFinalStateUnknown: Boolean) {
        //end::handler[]
        LOG.info("DELETED CONFIG MAP: {}", obj)
        deleted.add(obj)
        //tag::handler[]
    }
}
//end::handler[]
