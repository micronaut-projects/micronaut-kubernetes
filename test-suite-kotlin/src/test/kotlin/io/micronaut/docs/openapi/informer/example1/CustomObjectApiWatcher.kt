package io.micronaut.docs.openapi.informer.example1

import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.client.openapi.watcher.WatchEvent
import io.micronaut.kubernetes.client.openapi.watcher.annotation.KubernetesClientApiWatcher
import reactor.core.publisher.Flux

@Requires(property = "spec.name", value = "CustomObjectInformer1Spec")
//tag::get[]
@KubernetesClientApiWatcher
@Client("kubernetes")
interface CustomObjectApiWatcher {

    @Get("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects")
    @Consumes("application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor", "application/json;stream=watch", "application/vnd.kubernetes.protobuf;stream=watch", "application/cbor-seq")
    fun listCustomObject(
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
    ): Flux<WatchEvent<CustomObject>>
}
//end::get[]
