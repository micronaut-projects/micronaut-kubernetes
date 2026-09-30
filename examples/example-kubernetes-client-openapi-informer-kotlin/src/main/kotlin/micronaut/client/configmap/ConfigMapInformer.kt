//tag::class[]
package micronaut.client.configmap

import io.micronaut.context.annotation.Context
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
//end::class[]
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment

@Requires(env = [Environment.KUBERNETES])
//tag::class[]

@Context
class ConfigMapInformer(
    private val sharedIndexInformerFactory: SharedIndexInformerFactory,
    private val namespaceResolver: NamespaceResolver
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ConfigMapInformer::class.java)
    }

    @PostConstruct
    fun initialize() {
        val namespace = namespaceResolver.resolveNamespace()
        val sharedIndexInformer = sharedIndexInformerFactory.sharedIndexInformerFor(
            V1ConfigMap::class.java, namespace)
        sharedIndexInformer.addEventHandler(
            object : ResourceEventHandler<V1ConfigMap> {
                override fun onAdd(obj: V1ConfigMap) {
                    LOG.info("{} config map added!", obj.metadata?.name)
                }

                override fun onUpdate(oldObj: V1ConfigMap, newObj: V1ConfigMap) {
                    LOG.info("{} config map updated!", oldObj.metadata?.name)
                }

                override fun onDelete(obj: V1ConfigMap, deletedFinalStateUnknown: Boolean) {
                    LOG.info("{} config map deleted!", obj.metadata?.name)
                }
            })
    }
}
//end::class[]
