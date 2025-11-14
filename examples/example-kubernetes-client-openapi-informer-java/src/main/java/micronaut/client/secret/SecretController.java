package micronaut.client.secret;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver;

import java.util.List;

@Controller("secrets")
class SecretController {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;

    private final NamespaceResolver namespaceResolver;

    SecretController(SharedIndexInformerFactory sharedIndexInformerFactory, NamespaceResolver namespaceResolver) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.namespaceResolver = namespaceResolver;
    }

    @Get
    public List<V1Secret> all() {
        String namespace = namespaceResolver.resolveNamespace();
        SharedIndexInformer<V1Secret> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            V1Secret.class,
            namespace);
        Indexer<V1Secret> indexer = informer.getIndexer();
        return indexer.list();
    }

    @Get("/{name}")
    public V1Secret secret(String name) {
        String namespace = namespaceResolver.resolveNamespace();
        SharedIndexInformer<V1Secret> informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            V1Secret.class,
            namespace);
        Indexer<V1Secret> indexer = informer.getIndexer();
        return indexer.getByKey(namespace + "/" + name);
    }
}
