# tag::class[]
from java.util.function import Predicate
from jakarta.inject import Singleton
from micronaut.kubernetes.client.openapi.model import V1ConfigMap
# end::class[]
from micronaut.context.annotation import Requires
from micronaut.context.env import Environment
# TODO(python): the base class is the raw Predicate because the stub generated for Predicate[V1ConfigMap]
# copies the static Predicate.not(Predicate<? super T>) method with an invalid generic signature
# tag::class[]

# end::class[]

@Requires(env=Environment.KUBERNETES)
# tag::class[]
@Singleton
class OnAddFilter(Predicate):
    def test(self, config_map: V1ConfigMap) -> bool:
        annotations = config_map.getMetadata().getAnnotations()
        return annotations is not None and "io.micronaut.operator" in annotations
# end::class[]
