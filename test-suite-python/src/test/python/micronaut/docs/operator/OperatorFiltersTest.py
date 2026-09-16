from java.util import HashMap
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.docs.operator.OnAddFilter import OnAddFilter
from micronaut.docs.operator.OnDeleteFilter import OnDeleteFilter
from micronaut.docs.operator.OnUpdateFilter import OnUpdateFilter

try:
    from io.kubernetes.client.openapi.models import V1ConfigMap, V1ObjectMeta
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from kubernetes.client.openapi.models import V1ConfigMap, V1ObjectMeta


def config_map(annotations: dict[str, str] | None) -> V1ConfigMap:
    metadata = V1ObjectMeta().name("test").namespace("default")
    if annotations is not None:
        metadata.annotations(HashMap(annotations))
    return V1ConfigMap().metadata(metadata)


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
