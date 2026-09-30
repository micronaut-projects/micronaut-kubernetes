from java.util import HashMap
from micronaut.kubernetes.client.openapi.model import V1ConfigMap, V1ObjectMeta
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.operator.OnAddFilter import OnAddFilter
from micronaut.operator.OnDeleteFilter import OnDeleteFilter
from micronaut.operator.OnUpdateFilter import OnUpdateFilter


def config_map(annotations: dict[str, str] | None) -> V1ConfigMap:
    metadata = V1ObjectMeta()
    metadata.setName("test")
    metadata.setNamespace("default")
    if annotations is not None:
        metadata.setAnnotations(HashMap(annotations))
    cm = V1ConfigMap()
    cm.setMetadata(metadata)
    return cm


@MicronautTest
class OperatorFiltersTest:

    @Test
    def test_on_add_filter(self):
        assert OnAddFilter().test(config_map({"io.micronaut.operator": "processed"}))
        assert not OnAddFilter().test(config_map({"other": "value"}))
        assert not OnAddFilter().test(config_map(None))

    @Test
    def test_on_update_filter(self):
        assert OnUpdateFilter().test(config_map(None), config_map({"io.micronaut.operator": "processed"}))
        assert not OnUpdateFilter().test(config_map({"io.micronaut.operator": "processed"}), config_map(None))

    @Test
    def test_on_delete_filter(self):
        assert OnDeleteFilter().test(config_map({"io.micronaut.operator": "processed"}), False)
        assert not OnDeleteFilter().test(config_map(None), True)
