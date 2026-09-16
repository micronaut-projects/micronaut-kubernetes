# tag::reconciler[]
from java.util.function import BiPredicate
from jakarta.inject import Singleton

try:
    from io.kubernetes.client.openapi.models import V1ConfigMap
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from kubernetes.client.openapi.models import V1ConfigMap


@Singleton
class OnDeleteFilter(BiPredicate[V1ConfigMap, bool]):

    def test(self, v1_config_map: V1ConfigMap, deleted_final_state_unknown: bool) -> bool:
        annotations = v1_config_map.getMetadata().getAnnotations()
        if annotations is not None:
            return "io.micronaut.operator" in annotations
        return False
# end::reconciler[]
