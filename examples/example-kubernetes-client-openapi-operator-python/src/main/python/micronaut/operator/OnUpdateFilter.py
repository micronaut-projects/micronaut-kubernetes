# tag::class[]
from java.util.function import BiPredicate
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
class OnUpdateFilter(BiPredicate[V1ConfigMap, V1ConfigMap]):
    def test(self, old_config_map: V1ConfigMap, new_config_map: V1ConfigMap) -> bool:
        annotations = new_config_map.getMetadata().getAnnotations()
        return annotations is not None and "io.micronaut.operator" in annotations
# end::class[]
