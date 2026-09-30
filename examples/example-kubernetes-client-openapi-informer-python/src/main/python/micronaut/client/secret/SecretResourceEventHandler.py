# tag::class[]
import logging

from micronaut.kubernetes.client.openapi.informer.handler import Informer, ResourceEventHandler
from micronaut.kubernetes.client.openapi.model import V1Secret
# end::class[]
from micronaut.context.annotation import Requires
from micronaut.context.env import Environment
# tag::class[]

LOG = logging.getLogger(__name__)

# end::class[]

@Requires(env=Environment.KUBERNETES)
# tag::class[]
@Informer(apiType=V1Secret)  # <1>
class SecretResourceEventHandler(ResourceEventHandler[V1Secret]):  # <2>

    def onAdd(self, obj: V1Secret) -> None:
        LOG.info("%s secret added!", obj.getMetadata().getName())

    def onUpdate(self, old_obj: V1Secret, new_obj: V1Secret) -> None:
        LOG.info("%s secret updated!", old_obj.getMetadata().getName())

    def onDelete(self, obj: V1Secret, deleted_final_state_unknown: bool) -> None:
        LOG.info("%s secret deleted!", obj.getMetadata().getName())
# end::class[]
