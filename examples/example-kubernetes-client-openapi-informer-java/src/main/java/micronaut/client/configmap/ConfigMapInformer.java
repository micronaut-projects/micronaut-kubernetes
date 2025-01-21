package micronaut.client.configmap;

import io.micronaut.context.annotation.Context;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Context
public class ConfigMapInformer {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigMapInformer.class);

    static final String NAMESPACE = "test-informer-namespace";

    private final SharedIndexInformerFactory sharedIndexInformerFactory;

    ConfigMapInformer(SharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
    }

    @PostConstruct
    void initialize() {
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = sharedIndexInformerFactory.sharedIndexInformerFor(
            V1ConfigMap.class, NAMESPACE);
        sharedIndexInformer.addEventHandler(
            new ResourceEventHandler<>() {
                @Override
                public void onAdd(V1ConfigMap obj) {
                    LOG.info("{} config map added!", obj.getMetadata().getName());
                }

                @Override
                public void onUpdate(V1ConfigMap oldObj, V1ConfigMap newObj) {
                    LOG.info("{} config map updated!", oldObj.getMetadata().getName());
                }

                @Override
                public void onDelete(V1ConfigMap obj, boolean deletedFinalStateUnknown) {
                    LOG.info("{} config map deleted!", obj.getMetadata().getName());
                }
            });
    }
}
