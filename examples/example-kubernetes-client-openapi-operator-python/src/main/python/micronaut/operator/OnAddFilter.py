# tag::class[]
from java.util.function import Predicate
from jakarta.inject import Singleton
from micronaut.kubernetes.client.openapi.model import V1ConfigMap
# end::class[]
from micronaut.context.annotation import Requires
from micronaut.context.env import Environment
# tag::class[]

# end::class[]

@Requires(env=Environment.KUBERNETES)
# tag::class[]
@Singleton
class OnAddFilter(Predicate[V1ConfigMap]):
    def test(self, config_map: V1ConfigMap) -> bool:
        annotations = config_map.getMetadata().getAnnotations()
        return annotations is not None and "io.micronaut.operator" in annotations
# end::class[]
