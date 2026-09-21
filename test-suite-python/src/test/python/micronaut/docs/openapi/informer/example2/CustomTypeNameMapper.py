from jakarta.inject import Singleton
from micronaut.kubernetes.client.openapi.watcher.mapper import TypeNameMapper


# tag::get[]
@Singleton
class CustomTypeNameMapper(TypeNameMapper):
    def getMappings(self) -> dict[str, str]:
        return {
            "micronaut.docs.openapi.informer.example2.CustomObjectCollection": "micronaut.docs.openapi.informer.example2.CustomObject"
        }
# end::get[]
