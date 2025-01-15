package micronaut.client.configmap;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;

import java.util.List;

@Controller("config-maps")
class ConfigMapController {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;

    ConfigMapController(SharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
    }

    @Get
    List<V1ConfigMap> all() {
        SharedIndexInformer<V1ConfigMap> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            V1ConfigMap.class,
            ConfigMapInformer.NAMESPACE);
        Indexer<V1ConfigMap> indexer = informer.getIndexer();
        return indexer.list();
    }

    @Get("/{name}")
    V1ConfigMap configMap(String name) {
        SharedIndexInformer<V1ConfigMap> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            V1ConfigMap.class,
            ConfigMapInformer.NAMESPACE);
        Indexer<V1ConfigMap> indexer = informer.getIndexer();
        return indexer.getByKey(ConfigMapInformer.NAMESPACE + "/" + name);
    }
}
