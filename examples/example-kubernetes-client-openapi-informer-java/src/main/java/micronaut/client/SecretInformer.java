package micronaut.client;

import io.micronaut.context.annotation.Context;
import io.micronaut.kubernetes.client.openapi.informer.DefaultSharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.informer.ResourceEventHandler;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Context
@Singleton
public class SecretInformer {

    private static final Logger LOG = LoggerFactory.getLogger(SecretInformer.class);

    private final DefaultSharedIndexInformerFactory sharedIndexInformerFactory;

    SecretInformer(DefaultSharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
    }

    @PostConstruct
    void initialize() {
        createSecretInformer("test-informer-namespace");
    }

    private void createSecretInformer(String namespace) {
        SharedIndexInformer<V1Secret> sharedIndexInformer = sharedIndexInformerFactory.sharedIndexInformerFor(
            V1Secret.class, namespace);
        sharedIndexInformer.addEventHandler(
            new ResourceEventHandler<>() {
                @Override
                public void onAdd(V1Secret secret) {
                    LOG.info("{} secret added!", secret.getMetadata().getName());
                }

                @Override
                public void onUpdate(V1Secret oldSecret, V1Secret newSecret) {
                    LOG.info("{} => {} secret updated!", oldSecret.getMetadata().getName(), newSecret.getMetadata().getName());
                }

                @Override
                public void onDelete(V1Secret secret, boolean deletedFinalStateUnknown) {
                    LOG.info("{} secret deleted!", secret.getMetadata().getName());
                }
            });
    }
}
