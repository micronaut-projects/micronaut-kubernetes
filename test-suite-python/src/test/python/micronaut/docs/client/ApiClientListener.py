from java.util.concurrent import TimeUnit
from jakarta.inject import Singleton
from micronaut.context.event import BeanCreatedEvent, BeanCreatedEventListener

from io.kubernetes.client.openapi import ApiClient


# tag::listener[]
@Singleton
class ApiClientListener(BeanCreatedEventListener[ApiClient]):

    def onCreated(self, event: BeanCreatedEvent[ApiClient]) -> ApiClient:
        api_client = event.getBean()
        ok_http_client = api_client.getHttpClient().newBuilder() \
            .readTimeout(5345, TimeUnit.MILLISECONDS) \
            .build()
        api_client.setHttpClient(ok_http_client)
        return api_client
# end::listener[]
