from micronaut.kubernetes.client.openapi.common import KubernetesListObject
from micronaut.kubernetes.client.openapi.model import V1ListMeta
from micronaut.serde.annotation import Serdeable

from micronaut.docs.openapi.informer.example1.CustomObject import CustomObject


# tag::get[]
@Serdeable
class CustomObjectList(KubernetesListObject):

    def __init__(self, apiVersion: str | None = None, kind: str | None = None, metadata: V1ListMeta | None = None, items: list[CustomObject] | None = None):
        self._api_version = apiVersion
        self._kind = kind
        self._metadata = metadata
        self._items = items if items is not None else []

    def getApiVersion(self) -> str | None:
        return self._api_version

    def getKind(self) -> str | None:
        return self._kind

    def getMetadata(self) -> V1ListMeta | None:
        return self._metadata

    def getItems(self) -> list[CustomObject]:
        return self._items
# end::get[]
