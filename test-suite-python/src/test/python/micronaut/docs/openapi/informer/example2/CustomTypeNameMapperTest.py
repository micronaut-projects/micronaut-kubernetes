from typing import Annotated

from jakarta.inject import Inject
from micronaut.context import ApplicationContext
from micronaut.context.annotation import Property
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Disabled, Test

from micronaut.docs.openapi.informer.example2.CustomTypeNameMapper import CustomTypeNameMapper


# TODO(python): a Python TypeNameMapper bean is instantiated by the ApiReactorExecMethodProcessor before the
# GraalPy context is initialized ("GraalPy context has not been initialized")
@Disabled("TODO(python): Python TypeNameMapper beans are created before the GraalPy context is initialized")
@Property(name="spec.name", value="CustomTypeNameMapperSpec")
@MicronautTest
class CustomTypeNameMapperTest:
    context: Annotated[ApplicationContext, Inject] = None

    @Test
    def test_the_mapper_is_registered(self):
        mapper = self.context.getBean(CustomTypeNameMapper)
        mappings = mapper.getMappings()
        assert mappings.get("micronaut.docs.openapi.informer.example2.CustomObjectCollection") == "micronaut.docs.openapi.informer.example2.CustomObject"
        assert mappings.size() == 1
