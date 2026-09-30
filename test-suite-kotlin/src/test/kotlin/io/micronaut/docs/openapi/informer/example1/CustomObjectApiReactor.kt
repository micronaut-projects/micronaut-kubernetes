package io.micronaut.docs.openapi.informer.example1

import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.client.openapi.model.V1DeleteOptions
import io.micronaut.kubernetes.client.openapi.reactor.annotation.KubernetesClientApiReactor
import io.micronaut.kubernetes.client.openapi.response.DeleteResponse
import reactor.core.publisher.Mono

@Requires(property = "spec.name", value = "CustomObjectInformer1Spec")
//tag::get[]
@KubernetesClientApiReactor
@BootstrapContextCompatible
@Client("kubernetes")
interface CustomObjectApiReactor {

    @Post("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects")
    @Consumes("application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor")
    fun createNamespacedCustomObject(
        @PathVariable("namespace") namespace: String,
        @Body body: CustomObject,
        @QueryValue("pretty") @Nullable pretty: String?,
        @QueryValue("dryRun") @Nullable dryRun: String?,
        @QueryValue("fieldManager") @Nullable fieldManager: String?,
        @QueryValue("fieldValidation") @Nullable fieldValidation: String?
    ): Mono<CustomObject>

    @Delete("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects/{name}")
    @Consumes("application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor")
    fun deleteNamespacedCustomObject(
        @PathVariable("name") name: String,
        @PathVariable("namespace") namespace: String,
        @QueryValue("pretty") @Nullable pretty: String?,
        @QueryValue("dryRun") @Nullable dryRun: String?,
        @QueryValue("gracePeriodSeconds") @Nullable gracePeriodSeconds: Int?,
        @QueryValue("ignoreStoreReadErrorWithClusterBreakingPotential") @Nullable ignoreStoreReadErrorWithClusterBreakingPotential: Boolean?,
        @QueryValue("orphanDependents") @Nullable orphanDependents: Boolean?,
        @QueryValue("propagationPolicy") @Nullable propagationPolicy: String?,
        @Body @Nullable body: V1DeleteOptions?
    ): Mono<DeleteResponse<CustomObject>>

    @Get("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects")
    @Consumes("application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor", "application/json;stream=watch", "application/vnd.kubernetes.protobuf;stream=watch", "application/cbor-seq")
    fun listNamespacedCustomObject(
        @PathVariable("namespace") namespace: String,
        @QueryValue("pretty") @Nullable pretty: String?,
        @QueryValue("allowWatchBookmarks") @Nullable allowWatchBookmarks: Boolean?,
        @QueryValue("continue") @Nullable _continue: String?,
        @QueryValue("fieldSelector") @Nullable fieldSelector: String?,
        @QueryValue("labelSelector") @Nullable labelSelector: String?,
        @QueryValue("limit") @Nullable limit: Int?,
        @QueryValue("resourceVersion") @Nullable resourceVersion: String?,
        @QueryValue("resourceVersionMatch") @Nullable resourceVersionMatch: String?,
        @QueryValue("sendInitialEvents") @Nullable sendInitialEvents: Boolean?,
        @QueryValue("timeoutSeconds") @Nullable timeoutSeconds: Int?,
        @QueryValue("watch") @Nullable watch: Boolean?
    ): Mono<CustomObjectList>

    @Get("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects/{name}")
    @Consumes("application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor")
    fun readNamespacedCustomObject(
        @PathVariable("name") name: String,
        @PathVariable("namespace") namespace: String,
        @QueryValue("pretty") @Nullable pretty: String?
    ): Mono<CustomObject>

    @Put("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects/{name}")
    @Consumes("application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor")
    fun replaceNamespacedCustomObject(
        @PathVariable("name") name: String,
        @PathVariable("namespace") namespace: String,
        @Body body: CustomObject,
        @QueryValue("pretty") @Nullable pretty: String?,
        @QueryValue("dryRun") @Nullable dryRun: String?,
        @QueryValue("fieldManager") @Nullable fieldManager: String?,
        @QueryValue("fieldValidation") @Nullable fieldValidation: String?
    ): Mono<CustomObject>
}
//end::get[]
