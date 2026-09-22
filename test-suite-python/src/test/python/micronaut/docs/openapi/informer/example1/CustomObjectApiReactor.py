from typing import Annotated

from micronaut.context.annotation import BootstrapContextCompatible, Requires
from micronaut.http.annotation import Body, Consumes, Delete, Get, PathVariable, Post, Put, QueryValue
from micronaut.http.client.annotation import Client
from micronaut.kubernetes.client.openapi.model import V1DeleteOptions
from micronaut.kubernetes.client.openapi.reactor.annotation import KubernetesClientApiReactor
from micronaut.kubernetes.client.openapi.response import DeleteResponse
from reactor.core.publisher import Mono

from micronaut.docs.openapi.informer.example1.CustomObject import CustomObject
from micronaut.docs.openapi.informer.example1.CustomObjectList import CustomObjectList


@Requires(property="spec.name", value="CustomObjectInformer1Spec")
# tag::get[]
@KubernetesClientApiReactor
@BootstrapContextCompatible
@Client("kubernetes")
class CustomObjectApiReactor:

    @Post("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects")
    @Consumes(["application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor"])
    def create_namespaced_custom_object(
        self,
        namespace: Annotated[str, PathVariable("namespace")],
        body: Annotated[CustomObject, Body],
        pretty: Annotated[str | None, QueryValue("pretty")],
        dry_run: Annotated[str | None, QueryValue("dryRun")],
        field_manager: Annotated[str | None, QueryValue("fieldManager")],
        field_validation: Annotated[str | None, QueryValue("fieldValidation")]
    ) -> Mono[CustomObject]:
        ...

    @Delete("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects/{name}")
    @Consumes(["application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor"])
    def delete_namespaced_custom_object(
        self,
        name: Annotated[str, PathVariable("name")],
        namespace: Annotated[str, PathVariable("namespace")],
        pretty: Annotated[str | None, QueryValue("pretty")],
        dry_run: Annotated[str | None, QueryValue("dryRun")],
        grace_period_seconds: Annotated[int | None, QueryValue("gracePeriodSeconds")],
        ignore_store_read_error_with_cluster_breaking_potential: Annotated[bool | None, QueryValue("ignoreStoreReadErrorWithClusterBreakingPotential")],
        orphan_dependents: Annotated[bool | None, QueryValue("orphanDependents")],
        propagation_policy: Annotated[str | None, QueryValue("propagationPolicy")],
        body: Annotated[V1DeleteOptions | None, Body]
    ) -> Mono[DeleteResponse[CustomObject]]:
        ...

    @Get("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects")
    @Consumes(["application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor", "application/json;stream=watch", "application/vnd.kubernetes.protobuf;stream=watch", "application/cbor-seq"])
    def list_namespaced_custom_object(
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
    ) -> Mono[CustomObjectList]:
        ...

    @Get("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects/{name}")
    @Consumes(["application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor"])
    def read_namespaced_custom_object(
        self,
        name: Annotated[str, PathVariable("name")],
        namespace: Annotated[str, PathVariable("namespace")],
        pretty: Annotated[str | None, QueryValue("pretty")]
    ) -> Mono[CustomObject]:
        ...

    @Put("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects/{name}")
    @Consumes(["application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor"])
    def replace_namespaced_custom_object(
        self,
        name: Annotated[str, PathVariable("name")],
        namespace: Annotated[str, PathVariable("namespace")],
        body: Annotated[CustomObject, Body],
        pretty: Annotated[str | None, QueryValue("pretty")],
        dry_run: Annotated[str | None, QueryValue("dryRun")],
        field_manager: Annotated[str | None, QueryValue("fieldManager")],
        field_validation: Annotated[str | None, QueryValue("fieldValidation")]
    ) -> Mono[CustomObject]:
        ...
# end::get[]
