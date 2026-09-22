# tag::reconciler[]
from java.util.function import BiPredicate
from jakarta.inject import Singleton

from io.kubernetes.client.openapi.models import V1ConfigMap


@Singleton
class OnDeleteFilter(BiPredicate[V1ConfigMap, bool]):

    def test(self, v1_config_map: V1ConfigMap, deleted_final_state_unknown: bool) -> bool:
        annotations = v1_config_map.getMetadata().getAnnotations()
        if annotations is not None:
            return "io.micronaut.operator" in annotations
        return False
# end::reconciler[]
