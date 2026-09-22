# tag::class[]
from jakarta.inject import Singleton
from micronaut.kubernetes.client.reactor import CoreV1ApiReactorClient


@Singleton
class MyService:

    def __init__(self, core_v1_api_reactor_client: CoreV1ApiReactorClient):
        self.core_v1_api_reactor_client = core_v1_api_reactor_client

    def my_method(self, namespace: str) -> None:
        v1_pod_list = self.core_v1_api_reactor_client.listNamespacedPod(namespace).execute()
# end::class[]
