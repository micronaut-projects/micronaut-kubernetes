# tag::reconciler[]
from java.util.function import Predicate
from jakarta.inject import Singleton

from io.kubernetes.client.openapi.models import V1ConfigMap


@Singleton
class OnAddFilter(Predicate[V1ConfigMap]):

    def test(self, v1_config_map: V1ConfigMap) -> bool:
        annotations = v1_config_map.getMetadata().getAnnotations()
        if annotations is not None:
            return "io.micronaut.operator" in annotations
        return False
# end::reconciler[]
