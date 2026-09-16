package micronaut.client.configmap

import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver

@Requires(env = [Environment.KUBERNETES])
@Controller("config-maps")
class ConfigMapController(
    private val sharedIndexInformerFactory: SharedIndexInformerFactory,
    private val namespaceResolver: NamespaceResolver
) {

    @Get
    //tag::getAll[]
    fun all(): List<V1ConfigMap> {
        val namespace = namespaceResolver.resolveNamespace()
        val informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            V1ConfigMap::class.java,
            namespace)
        val indexer = informer!!.indexer
        return indexer.list()
    }
    //end::getAll[]

    @Get("/{name}")
    fun configMap(name: String): V1ConfigMap? {
        val namespace = namespaceResolver.resolveNamespace()
        //tag::get[]
        val informer = sharedIndexInformerFactory.getExistingSharedIndexInformer(
            V1ConfigMap::class.java,
            namespace)
        //end::get[]
        val indexer = informer!!.indexer
        return indexer.getByKey("$namespace/$name")
    }
}
