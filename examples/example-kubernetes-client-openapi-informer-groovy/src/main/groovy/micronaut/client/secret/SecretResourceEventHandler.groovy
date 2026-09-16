//tag::class[]
package micronaut.client.secret

import groovy.transform.CompileStatic
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
@Informer(apiType = V1Secret) // <1>
class SecretResourceEventHandler implements ResourceEventHandler<V1Secret> { // <2>

    private static final Logger LOG = LoggerFactory.getLogger(SecretResourceEventHandler)

    @Override
    void onAdd(V1Secret obj) {
        LOG.info("{} secret added!", obj.metadata.name)
    }

    @Override
    void onUpdate(V1Secret oldObj, V1Secret newObj) {
        LOG.info("{} secret updated!", oldObj.metadata.name)
    }

    @Override
    void onDelete(V1Secret obj, boolean deletedFinalStateUnknown) {
        LOG.info("{} secret deleted!", obj.metadata.name)
    }
}
//end::class[]
