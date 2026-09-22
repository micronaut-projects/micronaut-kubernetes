# tag::reconciler[]
from java.util.function import BiPredicate
from jakarta.inject import Singleton

from io.kubernetes.client.openapi.models import V1ConfigMap


@Singleton
class OnUpdateFilter(BiPredicate[V1ConfigMap, V1ConfigMap]):

    def test(self, old_obj: V1ConfigMap, new_obj: V1ConfigMap) -> bool:
        annotations = new_obj.getMetadata().getAnnotations()
        if annotations is not None:
            return "io.micronaut.operator" in annotations
        return False
# end::reconciler[]
