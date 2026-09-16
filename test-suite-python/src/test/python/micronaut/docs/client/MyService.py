# tag::class[]
from jakarta.inject import Singleton

try:
    from io.kubernetes.client.openapi.apis import CoreV1Api
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from kubernetes.client.openapi.apis import CoreV1Api


@Singleton
class MyService:

    def __init__(self, core_v1_api: CoreV1Api):
        self.core_v1_api = core_v1_api

    def my_method(self, namespace: str) -> None:
        v1_pod_list = self.core_v1_api.listNamespacedPod(namespace).execute()
# end::class[]
