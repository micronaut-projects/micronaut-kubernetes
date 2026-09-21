from typing import Annotated

from jakarta.inject import Inject
from micronaut.context import ApplicationContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.docs.openapi.informer.example2.CustomTypeNameMapper import CustomTypeNameMapper


@MicronautTest
class CustomTypeNameMapperTest:
    context: Annotated[ApplicationContext, Inject] = None

    @Test
    def test_the_mapper_is_registered(self):
        mapper = self.context.getBean(CustomTypeNameMapper)
        mappings = mapper.getMappings()
        assert mappings["micronaut.docs.openapi.informer.example2.CustomObjectCollection"] == "micronaut.docs.openapi.informer.example2.CustomObject"
        assert len(mappings) == 1
