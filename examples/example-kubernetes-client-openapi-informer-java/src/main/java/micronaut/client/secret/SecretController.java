package micronaut.client.secret;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;

import java.util.List;

@Controller("secrets")
class SecretController {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;

    SecretController(SharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
    }

    @Get
    public List<V1Secret> all() {
        SharedIndexInformer<V1Secret> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            V1Secret.class,
            SecretResourceEventHandler.NAMESPACE);
        Indexer<V1Secret> indexer = informer.getIndexer();
        return indexer.list();
    }

    @Get("/{name}")
    public V1Secret secret(String name) {
        SharedIndexInformer<V1Secret> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            V1Secret.class,
            SecretResourceEventHandler.NAMESPACE);
        Indexer<V1Secret> indexer = informer.getIndexer();
        return indexer.getByKey(SecretResourceEventHandler.NAMESPACE + "/" + name);
    }
}
