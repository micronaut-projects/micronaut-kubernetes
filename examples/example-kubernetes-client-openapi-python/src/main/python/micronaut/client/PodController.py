from typing import Annotated

from jakarta.inject import Inject
from jakarta.validation.constraints import NotNull
from micronaut.http.annotation import Controller, Get
from micronaut.kubernetes.client.openapi.api import CoreV1Api
from micronaut.scheduling import TaskExecutors
from micronaut.scheduling.annotation import ExecuteOn


@Controller("/pods")
@ExecuteOn(TaskExecutors.BLOCKING)
class PodController:

    core_v1_api: Annotated[CoreV1Api, Inject]

    @Get("/{namespace}/{name}")
    def get_pod(self, namespace: Annotated[str, NotNull], name: Annotated[str, NotNull]) -> str:
        v1_pod = self.core_v1_api.readNamespacedPod(name, namespace, None)
        return v1_pod.getStatus().getPhase()

    @Get("/{namespace}")
    def get_pods(self, namespace: Annotated[str, NotNull]) -> dict[str, str]:
        v1_pod_list = self.core_v1_api.listNamespacedPod(namespace, None, None, None, None, None, None, None, None, None, None, None, None)
        return {
            p.getMetadata().getName(): p.getStatus().getPhase()
            for p in v1_pod_list.getItems()
            if p.getStatus() is not None
        }
