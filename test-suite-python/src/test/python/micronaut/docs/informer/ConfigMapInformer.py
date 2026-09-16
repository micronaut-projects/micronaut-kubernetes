import logging

from micronaut.context.annotation import Requires
from micronaut.kubernetes.client.informer import Informer

try:
    from io.kubernetes.client.informer import ResourceEventHandler
    from io.kubernetes.client.openapi.models import V1ConfigMap, V1ConfigMapList
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from kubernetes.client.informer import ResourceEventHandler
    from kubernetes.client.openapi.models import V1ConfigMap, V1ConfigMapList

LOG = logging.getLogger(__name__)


@Requires(property="spec.name", value="ConfigMapInformerSpec")
# tag::handler[]
@Informer(apiType=V1ConfigMap, apiListType=V1ConfigMapList)  # <1>
class ConfigMapInformer(ResourceEventHandler[V1ConfigMap]):  # <2>

    # end::handler[]
    def __init__(self):
        self.added: list[V1ConfigMap] = []
        self.updated: list[V1ConfigMap] = []
        self.deleted: list[V1ConfigMap] = []

    # tag::handler[]
    def onAdd(self, obj: V1ConfigMap) -> None:
        # end::handler[]
        LOG.info("ADDED CONFIG MAP: %s", obj)
        self.added.append(obj)
        # tag::handler[]
        ...

    def onUpdate(self, old_obj: V1ConfigMap, new_obj: V1ConfigMap) -> None:
        # end::handler[]
        LOG.info("UPDATED CONFIG MAP: %s", new_obj)
        self.updated.append(new_obj)
        # tag::handler[]
        ...

    def onDelete(self, obj: V1ConfigMap, deleted_final_state_unknown: bool) -> None:
        # end::handler[]
        LOG.info("DELETED CONFIG MAP: %s", obj)
        self.deleted.append(obj)
        # tag::handler[]
        ...
# end::handler[]
