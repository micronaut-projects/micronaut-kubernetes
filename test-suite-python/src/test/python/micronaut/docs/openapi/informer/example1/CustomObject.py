from dataclasses import dataclass

from micronaut.kubernetes.client.openapi.common import KubernetesObject
from micronaut.kubernetes.client.openapi.model import V1ObjectMeta
from micronaut.serde.annotation import Serdeable


# tag::get[]
@Serdeable
@dataclass
class CustomObject(KubernetesObject):
    apiVersion: str | None = None
    kind: str | None = None
    metadata: V1ObjectMeta | None = None
    value: str | None = None
# end::get[]
