from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.kubernetes.client.openapi.watcher.mapper import TypeNameMapper


# TODO(python): TypeNameMapper beans are collected by TypeNameResolver while the ApiReactorExecMethodProcessor
# runs during context start, before the GraalPy context bean is initialized, so a Python mapper can only be
# enabled for the CustomTypeNameMapperTest (which is disabled for the same reason).
@Requires(property="spec.name", value="CustomTypeNameMapperSpec")
# tag::get[]
@Singleton
class CustomTypeNameMapper(TypeNameMapper):
    def getMappings(self) -> dict[str, str]:
        return {
            "micronaut.docs.openapi.informer.example2.CustomObjectCollection": "micronaut.docs.openapi.informer.example2.CustomObject"
        }
# end::get[]
