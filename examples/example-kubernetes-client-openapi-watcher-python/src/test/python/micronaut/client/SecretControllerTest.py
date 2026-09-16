from typing import Annotated

from jakarta.inject import Inject
from micronaut.context import ApplicationContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.client.SecretController import SecretController


@MicronautTest
class SecretControllerTest:
    context: Annotated[ApplicationContext, Inject] = None

    @Test
    def test_core_v1_api_watcher_is_injected(self):
        controller = self.context.getBean(SecretController).asPolyglotValue()
        assert controller.core_v1_api_watcher is not None
