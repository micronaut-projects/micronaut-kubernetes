# tag::class[]
from jakarta.inject import Singleton
from micronaut.kubernetes.client.rxjava3 import CoreV1ApiRxClient


@Singleton
class MyService:

    def __init__(self, core_v1_api_rx_client: CoreV1ApiRxClient):
        self.core_v1_api_rx_client = core_v1_api_rx_client

    def my_method(self, namespace: str) -> None:
        v1_pod_list = self.core_v1_api_rx_client.listNamespacedPod(namespace).execute()
# end::class[]
