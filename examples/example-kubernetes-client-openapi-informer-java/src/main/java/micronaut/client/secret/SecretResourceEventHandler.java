package micronaut.client.secret;

import io.micronaut.kubernetes.client.openapi.informer.handler.Informer;
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Informer(apiType = V1Secret.class, namespace = SecretResourceEventHandler.NAMESPACE) // <1>
class SecretResourceEventHandler implements ResourceEventHandler<V1Secret> { // <2>

    private static final Logger LOG = LoggerFactory.getLogger(SecretResourceEventHandler.class);

    static final String NAMESPACE = "test-informer-namespace";

    @Override
    public void onAdd(V1Secret obj) {
        LOG.info("{} secret added!", obj.getMetadata().getName());
    }

    @Override
    public void onUpdate(V1Secret oldObj, V1Secret newObj) {
        LOG.info("{} secret updated!", oldObj.getMetadata().getName());
    }

    @Override
    public void onDelete(V1Secret obj, boolean deletedFinalStateUnknown) {
        LOG.info("{} secret deleted!", obj.getMetadata().getName());
    }
}
