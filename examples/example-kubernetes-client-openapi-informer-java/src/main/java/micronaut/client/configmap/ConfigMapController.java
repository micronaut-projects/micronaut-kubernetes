package micronaut.client.configmap;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver;

import java.util.List;

@Controller("config-maps")
class ConfigMapController {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;

    private final NamespaceResolver namespaceResolver;

    ConfigMapController(SharedIndexInformerFactory sharedIndexInformerFactory, NamespaceResolver namespaceResolver) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.namespaceResolver = namespaceResolver;
    }

    @Get
    //tag::getAll[]
    List<V1ConfigMap> all() {
        String namespace = namespaceResolver.resolveNamespace();
        SharedIndexInformer<V1ConfigMap> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            V1ConfigMap.class,
            namespace);
        Indexer<V1ConfigMap> indexer = informer.getIndexer();
        return indexer.list();
    }
    //end::getAll[]

    @Get("/{name}")
    V1ConfigMap configMap(String name) {
        String namespace = namespaceResolver.resolveNamespace();
        //tag::get[]
        SharedIndexInformer<V1ConfigMap> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            V1ConfigMap.class,
            namespace);
        //end::get[]
        Indexer<V1ConfigMap> indexer = informer.getIndexer();
        return indexer.getByKey(namespace + "/" + name);
    }
}
