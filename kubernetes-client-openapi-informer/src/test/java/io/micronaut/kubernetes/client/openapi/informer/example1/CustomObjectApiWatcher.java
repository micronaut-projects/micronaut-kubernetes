package io.micronaut.kubernetes.client.openapi.informer.example1;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.kubernetes.client.openapi.watcher.WatchEvent;
import io.micronaut.kubernetes.client.openapi.watcher.annotation.KubernetesClientApiWatcher;
import reactor.core.publisher.Flux;

@Requires(property = "spec.name", value = "CustomObjectInformer1Spec")
//tag::get[]
@KubernetesClientApiWatcher
@Client("kubernetes")
public interface CustomObjectApiWatcher {

    @Get("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects")
    @Consumes({"application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor", "application/json;stream=watch", "application/vnd.kubernetes.protobuf;stream=watch", "application/cbor-seq"})
    Flux<WatchEvent<CustomObject>> listCustomObject(
        @PathVariable("namespace") String namespace,
        @QueryValue("pretty") @Nullable String pretty,
        @QueryValue("allowWatchBookmarks") @Nullable Boolean allowWatchBookmarks,
        @QueryValue("continue") @Nullable String _continue,
        @QueryValue("fieldSelector") @Nullable String fieldSelector,
        @QueryValue("labelSelector") @Nullable String labelSelector,
        @QueryValue("limit") @Nullable Integer limit,
        @QueryValue("resourceVersion") @Nullable String resourceVersion,
        @QueryValue("resourceVersionMatch") @Nullable String resourceVersionMatch,
        @QueryValue("sendInitialEvents") @Nullable Boolean sendInitialEvents,
        @QueryValue("timeoutSeconds") @Nullable Integer timeoutSeconds,
        @QueryValue("watch") @Nullable Boolean watch
    );
}
//end::get[]
