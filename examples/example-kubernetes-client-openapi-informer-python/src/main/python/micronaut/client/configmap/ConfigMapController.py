from micronaut.context.annotation import Requires
from micronaut.context.env import Environment
from micronaut.http.annotation import Controller, Get
from micronaut.kubernetes.client.openapi.informer import SharedIndexInformerFactory
from micronaut.kubernetes.client.openapi.model import V1ConfigMap
from micronaut.kubernetes.client.openapi.resolver import NamespaceResolver


@Requires(env=Environment.KUBERNETES)
@Controller("config-maps")
class ConfigMapController:

    def __init__(self, shared_index_informer_factory: SharedIndexInformerFactory, namespace_resolver: NamespaceResolver):
        self.shared_index_informer_factory = shared_index_informer_factory
        self.namespace_resolver = namespace_resolver

    @Get
    # tag::getAll[]
    def all(self) -> list[V1ConfigMap]:
        namespace = self.namespace_resolver.resolveNamespace()
        informer = self.shared_index_informer_factory.getExistingSharedIndexInformer(
            V1ConfigMap,
            namespace)
        indexer = informer.getIndexer()
        return indexer.list()
    # end::getAll[]

    @Get("/{name}")
    def config_map(self, name: str) -> V1ConfigMap | None:
        namespace = self.namespace_resolver.resolveNamespace()
        # tag::get[]
        informer = self.shared_index_informer_factory.getExistingSharedIndexInformer(
            V1ConfigMap,
            namespace)
        # end::get[]
        indexer = informer.getIndexer()
        return indexer.getByKey(namespace + "/" + name)
