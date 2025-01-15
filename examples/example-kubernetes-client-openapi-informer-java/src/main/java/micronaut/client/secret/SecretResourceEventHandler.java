package micronaut.client.secret;

import io.micronaut.context.annotation.Context;
import io.micronaut.kubernetes.client.openapi.informer.Informer;
import io.micronaut.kubernetes.client.openapi.informer.ResourceEventHandler;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Context
@Informer(apiType = V1Secret.class, namespace = SecretResourceEventHandler.NAMESPACE)
public class SecretResourceEventHandler implements ResourceEventHandler<V1Secret> {

    private static final Logger LOG = LoggerFactory.getLogger(SecretResourceEventHandler.class);

    public static final String NAMESPACE = "test-informer-namespace";

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
