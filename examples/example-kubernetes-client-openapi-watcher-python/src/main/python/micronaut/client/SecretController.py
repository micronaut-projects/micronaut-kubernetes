import logging
from typing import Annotated

from jakarta.inject import Inject
from jakarta.validation.constraints import NotNull
from micronaut.http.annotation import Controller, Get
from micronaut.kubernetes.client.openapi.watcher.api import CoreV1ApiWatcher

LOG = logging.getLogger(__name__)


@Controller("/secrets")
class SecretController:

    core_v1_api_watcher: Annotated[CoreV1ApiWatcher, Inject]

    @Get("/{namespace}")
    def start_watching_secrets(self, namespace: Annotated[str, NotNull]) -> None:
        self.core_v1_api_watcher.listNamespacedSecret(namespace, None, None, None, None, None, None, None, None, None, None, None, True) \
            .subscribe(lambda event: LOG.info(str(event)))
