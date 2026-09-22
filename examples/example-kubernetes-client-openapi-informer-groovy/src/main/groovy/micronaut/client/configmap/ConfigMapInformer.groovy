//tag::class[]
package micronaut.client.configmap

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Context
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
@Context
class ConfigMapInformer {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigMapInformer)

    private final SharedIndexInformerFactory sharedIndexInformerFactory

    private final NamespaceResolver namespaceResolver

    ConfigMapInformer(SharedIndexInformerFactory sharedIndexInformerFactory, NamespaceResolver namespaceResolver) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory
        this.namespaceResolver = namespaceResolver
    }

    @PostConstruct
    void initialize() {
        String namespace = namespaceResolver.resolveNamespace()
        SharedIndexInformer<V1ConfigMap> sharedIndexInformer = sharedIndexInformerFactory.sharedIndexInformerFor(
            V1ConfigMap, namespace)
        sharedIndexInformer.addEventHandler(
            new ResourceEventHandler<V1ConfigMap>() {
                @Override
                void onAdd(V1ConfigMap obj) {
                    LOG.info("{} config map added!", obj.metadata.name)
                }

                @Override
                void onUpdate(V1ConfigMap oldObj, V1ConfigMap newObj) {
                    LOG.info("{} config map updated!", oldObj.metadata.name)
                }

                @Override
                void onDelete(V1ConfigMap obj, boolean deletedFinalStateUnknown) {
                    LOG.info("{} config map deleted!", obj.metadata.name)
                }
            })
    }
}
//end::class[]
