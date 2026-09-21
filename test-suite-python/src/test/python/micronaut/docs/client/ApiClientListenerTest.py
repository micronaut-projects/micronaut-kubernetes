from typing import Annotated

from jakarta.inject import Inject
from micronaut.context import ApplicationContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from io.kubernetes.client.openapi import ApiClient


@MicronautTest
class ApiClientListenerTest:
    context: Annotated[ApplicationContext, Inject] = None

    @Test
    def test_the_listener_customizes_the_ok_http_client(self):
        api_client = self.context.getBean(ApiClient)
        assert api_client.getHttpClient().readTimeoutMillis() == 5345
