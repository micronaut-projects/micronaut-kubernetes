from typing import Annotated

from micronaut.context.annotation import Requires
from micronaut.http.annotation import Consumes, Get, PathVariable, QueryValue
from micronaut.http.client.annotation import Client
from micronaut.kubernetes.client.openapi.watcher import WatchEvent
from micronaut.kubernetes.client.openapi.watcher.annotation import KubernetesClientApiWatcher
from reactor.core.publisher import Flux

from micronaut.docs.openapi.informer.example1.CustomObject import CustomObject


@Requires(property="spec.name", value="CustomObjectInformer1Spec")
# tag::get[]
@KubernetesClientApiWatcher
@Client("kubernetes")
class CustomObjectApiWatcher:

    @Get("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects")
    @Consumes(["application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor", "application/json;stream=watch", "application/vnd.kubernetes.protobuf;stream=watch", "application/cbor-seq"])
    def list_custom_object(
        self,
        namespace: Annotated[str, PathVariable("namespace")],
        pretty: Annotated[str | None, QueryValue("pretty")],
        allow_watch_bookmarks: Annotated[bool | None, QueryValue("allowWatchBookmarks")],
        continue_: Annotated[str | None, QueryValue("continue")],
        field_selector: Annotated[str | None, QueryValue("fieldSelector")],
        label_selector: Annotated[str | None, QueryValue("labelSelector")],
        limit: Annotated[int | None, QueryValue("limit")],
        resource_version: Annotated[str | None, QueryValue("resourceVersion")],
        resource_version_match: Annotated[str | None, QueryValue("resourceVersionMatch")],
        send_initial_events: Annotated[bool | None, QueryValue("sendInitialEvents")],
        timeout_seconds: Annotated[int | None, QueryValue("timeoutSeconds")],
        watch: Annotated[bool | None, QueryValue("watch")]
    ) -> Flux[WatchEvent[CustomObject]]:
        ...
# end::get[]
