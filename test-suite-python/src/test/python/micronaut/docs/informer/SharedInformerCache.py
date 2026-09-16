from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.kubernetes.client.informer import SharedIndexInformerFactory

try:
    from io.kubernetes.client.openapi.models import V1ConfigMap
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from kubernetes.client.openapi.models import V1ConfigMap


@Requires(property="spec.name", value="ConfigMapInformerSpec")
# tag::cache[]
@Singleton
class SharedInformerCache:

    def __init__(self, shared_index_informer_factory: SharedIndexInformerFactory):
        self.shared_index_informer_factory = shared_index_informer_factory

    def get_config_maps(self, namespace: str) -> list[V1ConfigMap] | None:
        """Get all config maps from informer from namespace."""
        shared_index_informer = self.shared_index_informer_factory.getExistingSharedIndexInformer(namespace, V1ConfigMap)
        if shared_index_informer is not None:
            indexer = shared_index_informer.getIndexer()
            return indexer.list()
        else:
            return None
# end::cache[]
