from typing import Annotated

from jakarta.inject import Inject
from micronaut.kubernetes.client.openapi.model import V1ObjectMeta
from micronaut.serde import ObjectMapper
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Disabled, Test

from micronaut.docs.openapi.informer.example1.CustomObject import CustomObject
from micronaut.docs.openapi.informer.example1.CustomObjectList import CustomObjectList


@MicronautTest
class CustomObjectTest:
    object_mapper: Annotated[ObjectMapper, Inject]

    # TODO(python): the introspection of a Python class is built from its attributes, so a class that
    # implements KubernetesObject through explicit getters (dataclass attributes would clash with the
    # interface getters) is serialized as an empty object
    @Disabled("TODO(python): @Serdeable ignores the interface getters of a Python class without attributes")
    @Test
    def test_custom_object_is_serialized(self):
        custom_object = CustomObject("custom.test.io/v1", "CustomObject", V1ObjectMeta().name("test").namespace("default"), "value")
        json = self.object_mapper.writeValueAsString(custom_object)
        assert '"apiVersion":"custom.test.io/v1"' in json
        assert '"kind":"CustomObject"' in json
        assert '"value":"value"' in json

    @Test
    def test_custom_object_list_exposes_its_items(self):
        custom_object_list = CustomObjectList("custom.test.io/v1", "CustomObjectList", None, [CustomObject(kind="CustomObject")])
        assert custom_object_list.getKind() == "CustomObjectList"
        assert [item.getKind() for item in custom_object_list.getItems()] == ["CustomObject"]
