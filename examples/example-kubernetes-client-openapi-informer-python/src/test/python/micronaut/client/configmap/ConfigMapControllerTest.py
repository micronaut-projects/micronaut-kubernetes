from micronaut.kubernetes.client.openapi.model import V1ConfigMap, V1ObjectMeta
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.client.configmap.ConfigMapController import ConfigMapController


class InMemoryIndexer:
    """Stands in for the informer cache, keyed by namespace/name."""

    def __init__(self, config_maps: list[V1ConfigMap]):
        self.config_maps = config_maps

    def list(self) -> list[V1ConfigMap]:
        return self.config_maps

    def getByKey(self, key: str) -> V1ConfigMap | None:
        for config_map in self.config_maps:
            metadata = config_map.getMetadata()
            if metadata.getNamespace() + "/" + metadata.getName() == key:
                return config_map
        return None


class InMemoryInformer:
    def __init__(self, indexer: InMemoryIndexer):
        self.indexer = indexer

    def getIndexer(self) -> InMemoryIndexer:
        return self.indexer


class InMemoryInformerFactory:
    def __init__(self, informer: InMemoryInformer):
        self.informer = informer

    def getExistingSharedIndexInformer(self, api_type, namespace: str) -> InMemoryInformer:
        return self.informer


class FixedNamespaceResolver:
    def resolveNamespace(self) -> str:
        return "default"


def config_map(name: str) -> V1ConfigMap:
    return V1ConfigMap().metadata(V1ObjectMeta().name(name).namespace("default"))


def controller(*config_maps: V1ConfigMap) -> ConfigMapController:
    factory = InMemoryInformerFactory(InMemoryInformer(InMemoryIndexer(list(config_maps))))
    return ConfigMapController(factory, FixedNamespaceResolver())


@MicronautTest
class ConfigMapControllerTest:

    @Test
    def test_all_returns_the_cached_config_maps(self):
        names = [cm.getMetadata().getName() for cm in controller(config_map("first"), config_map("second")).all()]
        assert names == ["first", "second"]

    @Test
    def test_config_map_is_looked_up_by_key(self):
        ctrl = controller(config_map("first"))
        assert ctrl.config_map("first").getMetadata().getName() == "first"
        assert ctrl.config_map("missing") is None
