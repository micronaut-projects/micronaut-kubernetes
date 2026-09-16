from micronaut.kubernetes.client.openapi.common import KubernetesObject
from micronaut.kubernetes.client.openapi.model import V1ObjectMeta
from micronaut.serde.annotation import Serdeable


# tag::get[]
@Serdeable
class CustomObject(KubernetesObject):

    def __init__(self, apiVersion: str | None = None, kind: str | None = None, metadata: V1ObjectMeta | None = None, value: str | None = None):
        self._api_version = apiVersion
        self._kind = kind
        self._metadata = metadata
        self._value = value

    def getApiVersion(self) -> str | None:
        return self._api_version

    def getKind(self) -> str | None:
        return self._kind

    def getMetadata(self) -> V1ObjectMeta | None:
        return self._metadata

    def getValue(self) -> str | None:
        return self._value
# end::get[]
