from dataclasses import dataclass, field

from micronaut.kubernetes.client.openapi.common import KubernetesListObject
from micronaut.kubernetes.client.openapi.model import V1ListMeta
from micronaut.serde.annotation import Serdeable

from micronaut.docs.openapi.informer.example1.CustomObject import CustomObject


# tag::get[]
@Serdeable
@dataclass
class CustomObjectList(KubernetesListObject):
    apiVersion: str | None = None
    kind: str | None = None
    metadata: V1ListMeta | None = None
    items: list[CustomObject] = field(default_factory=list)
# end::get[]
