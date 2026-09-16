from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.docs.informer.ConfigMapInformer import ConfigMapInformer

try:
    from io.kubernetes.client.openapi.models import V1ConfigMap, V1ObjectMeta
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from kubernetes.client.openapi.models import V1ConfigMap, V1ObjectMeta


def config_map(name: str) -> V1ConfigMap:
    return V1ConfigMap().metadata(V1ObjectMeta().name(name).namespace("default"))


@MicronautTest
class ConfigMapInformerTest:

    @Test
    def test_the_handler_records_the_events(self):
        informer = ConfigMapInformer()

        informer.onAdd(config_map("added"))
        informer.onUpdate(config_map("added"), config_map("updated"))
        informer.onDelete(config_map("deleted"), False)

        assert [cm.getMetadata().getName() for cm in informer.added] == ["added"]
        assert [cm.getMetadata().getName() for cm in informer.updated] == ["updated"]
        assert [cm.getMetadata().getName() for cm in informer.deleted] == ["deleted"]
