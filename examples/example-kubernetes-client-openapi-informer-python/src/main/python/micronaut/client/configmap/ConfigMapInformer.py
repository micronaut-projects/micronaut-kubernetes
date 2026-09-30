# tag::class[]
import logging

from jakarta.annotation import PostConstruct
from micronaut.context.annotation import Context
from micronaut.kubernetes.client.openapi.informer import SharedIndexInformerFactory
from micronaut.kubernetes.client.openapi.informer.handler import ResourceEventHandler
from micronaut.kubernetes.client.openapi.model import V1ConfigMap
from micronaut.kubernetes.client.openapi.resolver import NamespaceResolver
# end::class[]
from micronaut.context.annotation import Requires
from micronaut.context.env import Environment
# tag::class[]

LOG = logging.getLogger(__name__)

# end::class[]

@Requires(env=Environment.KUBERNETES)
# tag::class[]
@Context
class ConfigMapInformer:

    def __init__(self, shared_index_informer_factory: SharedIndexInformerFactory, namespace_resolver: NamespaceResolver):
        self.shared_index_informer_factory = shared_index_informer_factory
        self.namespace_resolver = namespace_resolver

    @PostConstruct
    def initialize(self) -> None:
        namespace = self.namespace_resolver.resolveNamespace()
        shared_index_informer = self.shared_index_informer_factory.sharedIndexInformerFor(
            V1ConfigMap, namespace)
        shared_index_informer.addEventHandler(self.ConfigMapEventHandler())

    class ConfigMapEventHandler(ResourceEventHandler[V1ConfigMap]):

        def onAdd(self, obj: V1ConfigMap) -> None:
            LOG.info("%s config map added!", obj.getMetadata().getName())

        def onUpdate(self, old_obj: V1ConfigMap, new_obj: V1ConfigMap) -> None:
            LOG.info("%s config map updated!", old_obj.getMetadata().getName())

        def onDelete(self, obj: V1ConfigMap, deleted_final_state_unknown: bool) -> None:
            LOG.info("%s config map deleted!", obj.getMetadata().getName())
# end::class[]
