from typing import Annotated

from jakarta.inject import Inject
from micronaut.context import ApplicationContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.client.PodController import PodController


@MicronautTest
class PodControllerTest:
    context: Annotated[ApplicationContext, Inject] = None

    @Test
    def test_core_v1_api_is_injected(self):
        pod_controller = self.context.getBean(PodController).asPolyglotValue()
        assert pod_controller.core_v1_api is not None
