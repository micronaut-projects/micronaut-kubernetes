from typing import Annotated

from jakarta.inject import Inject
from micronaut.kubernetes.client.openapi.model import V1ObjectMeta
from micronaut.serde import ObjectMapper
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.docs.openapi.informer.example1.CustomObject import CustomObject
from micronaut.docs.openapi.informer.example1.CustomObjectList import CustomObjectList


@MicronautTest
class CustomObjectTest:
    object_mapper: Annotated[ObjectMapper, Inject]

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
        assert custom_object_list.kind == "CustomObjectList"
        assert [item.kind for item in custom_object_list.items] == ["CustomObject"]
