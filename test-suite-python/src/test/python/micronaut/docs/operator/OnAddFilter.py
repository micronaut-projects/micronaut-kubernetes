# tag::reconciler[]
from java.util.function import Predicate
from jakarta.inject import Singleton

try:
    from io.kubernetes.client.openapi.models import V1ConfigMap
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from kubernetes.client.openapi.models import V1ConfigMap

# end::reconciler[]

# TODO(python): the base class is the raw Predicate because the stub generated for Predicate[V1ConfigMap]
# copies the static Predicate.not(Predicate<? super T>) method with an invalid generic signature
# tag::reconciler[]
@Singleton
class OnAddFilter(Predicate):

    def test(self, v1_config_map: V1ConfigMap) -> bool:
        annotations = v1_config_map.getMetadata().getAnnotations()
        if annotations is not None:
            return "io.micronaut.operator" in annotations
        return False
# end::reconciler[]
