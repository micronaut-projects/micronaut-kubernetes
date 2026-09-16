# tag::reconciler[]
from java.util.function import BiPredicate
from jakarta.inject import Singleton

try:
    from io.kubernetes.client.openapi.models import V1ConfigMap
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from kubernetes.client.openapi.models import V1ConfigMap


@Singleton
class OnUpdateFilter(BiPredicate[V1ConfigMap, V1ConfigMap]):

    def test(self, old_obj: V1ConfigMap, new_obj: V1ConfigMap) -> bool:
        annotations = new_obj.getMetadata().getAnnotations()
        if annotations is not None:
            return "io.micronaut.operator" in annotations
        return False
# end::reconciler[]
