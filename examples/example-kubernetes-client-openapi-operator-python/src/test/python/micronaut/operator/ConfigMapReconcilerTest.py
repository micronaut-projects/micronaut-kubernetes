from java.util import Optional
from micronaut.kubernetes.client.openapi.model import V1ConfigMap, V1ObjectMeta
from micronaut.kubernetes.client.openapi.operator.controller.reconciler import Request
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.operator.ConfigMapReconciler import ConfigMapReconciler


class RecordingCoreV1Api:
    """Records the config maps the reconciler replaces instead of calling the Kubernetes API."""

    def __init__(self):
        self.replaced = []

    def replaceNamespacedConfigMap(self, name, namespace, body, pretty, dry_run, field_manager, field_validation):
        self.replaced.append((name, namespace, body))
        return body


class SingleEntryLister:
    """Stands in for the OperatorResourceLister backed by the informer cache."""

    def __init__(self, config_map: V1ConfigMap):
        self.config_map = config_map

    def get(self, request: Request) -> Optional[V1ConfigMap]:
        metadata = self.config_map.getMetadata()
        if metadata.getName() == request.name() and metadata.getNamespace() == request.namespace():
            return Optional.of(self.config_map)
        return Optional.empty()


def config_map(name: str, namespace: str) -> V1ConfigMap:
    metadata = V1ObjectMeta()
    metadata.setName(name)
    metadata.setNamespace(namespace)
    cm = V1ConfigMap()
    cm.setMetadata(metadata)
    return cm


@MicronautTest
class ConfigMapReconcilerTest:

    @Test
    def test_reconcile_annotates_the_config_map(self):
        cm = config_map("my-config", "default")
        lister = SingleEntryLister(cm)
        api = RecordingCoreV1Api()
        reconciler = ConfigMapReconciler(api)

        result = reconciler.reconcile(Request("my-config", "default"), lister)

        assert not result.requeue()
        assert len(api.replaced) == 1
        assert cm.getMetadata().getAnnotations().get("io.micronaut.operator") == "processed"

        # the annotation is already present, so the second reconciliation does not update the resource
        result = reconciler.reconcile(Request("my-config", "default"), lister)
        assert not result.requeue()
        assert len(api.replaced) == 1

    @Test
    def test_reconcile_of_a_deleted_config_map(self):
        lister = SingleEntryLister(config_map("other", "default"))
        api = RecordingCoreV1Api()

        result = ConfigMapReconciler(api).reconcile(Request("my-config", "default"), lister)

        assert not result.requeue()
        assert len(api.replaced) == 0
