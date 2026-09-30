from typing import Annotated

from jakarta.inject import Inject
from jakarta.validation.constraints import NotNull
from micronaut.http.annotation import Controller, Get
from micronaut.kubernetes.client.openapi.reactor.api import CoreV1ApiReactor
from reactor.core.publisher import Mono


@Controller("/pods")
class PodController:

    core_v1_api_reactor: Annotated[CoreV1ApiReactor, Inject]

    @Get("/{namespace}/{name}")
    def get_pod(self, namespace: Annotated[str, NotNull], name: Annotated[str, NotNull]) -> Mono[str]:
        return self.core_v1_api_reactor.readNamespacedPod(name, namespace, None) \
            .map(lambda it: it.getStatus().getPhase())

    @Get("/{namespace}")
    def get_pods(self, namespace: Annotated[str, NotNull]) -> Mono[dict[str, str]]:
        return self.core_v1_api_reactor.listNamespacedPod(namespace, None, None, None, None, None, None, None, None, None, None, None, None) \
            .map(lambda it: {
                p.getMetadata().getName(): p.getStatus().getPhase()
                for p in it.getItems()
                if p.getStatus() is not None
            })
