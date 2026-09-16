//tag::class[]
package micronaut.client.secret

import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import org.slf4j.LoggerFactory
//end::class[]
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment

@Requires(env = [Environment.KUBERNETES])
//tag::class[]

@Informer(apiType = V1Secret::class) // <1>
class SecretResourceEventHandler : ResourceEventHandler<V1Secret> { // <2>

    companion object {
        private val LOG = LoggerFactory.getLogger(SecretResourceEventHandler::class.java)
    }

    override fun onAdd(obj: V1Secret) {
        LOG.info("{} secret added!", obj.metadata?.name)
    }

    override fun onUpdate(oldObj: V1Secret, newObj: V1Secret) {
        LOG.info("{} secret updated!", oldObj.metadata?.name)
    }

    override fun onDelete(obj: V1Secret, deletedFinalStateUnknown: Boolean) {
        LOG.info("{} secret deleted!", obj.metadata?.name)
    }
}
//end::class[]
