package io.micronaut.kubernetes.client.openapi.informer.example2;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.kubernetes.client.openapi.model.V1DeleteOptions;
import io.micronaut.kubernetes.client.openapi.reactor.annotation.KubernetesClientApiReactor;
import io.micronaut.kubernetes.client.openapi.response.DeleteResponse;
import reactor.core.publisher.Mono;

@Requires(property = "spec.name", value = "CustomObjectInformer2Spec")
@KubernetesClientApiReactor
@BootstrapContextCompatible
@Client("kubernetes")
public interface CustomObjectApiReactor {

    @Post("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects")
    @Consumes({"application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor"})
    Mono<CustomObject> createNamespacedCustomObject(
        @PathVariable("namespace") String namespace,
        @Body CustomObject body,
        @QueryValue("pretty") @Nullable String pretty,
        @QueryValue("dryRun") @Nullable String dryRun,
        @QueryValue("fieldManager") @Nullable String fieldManager,
        @QueryValue("fieldValidation") @Nullable String fieldValidation
    );

    @Delete("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects/{name}")
    @Consumes({"application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor"})
    Mono<DeleteResponse<CustomObject>> deleteNamespacedCustomObject(
        @PathVariable("name") String name,
        @PathVariable("namespace") String namespace,
        @QueryValue("pretty") @Nullable String pretty,
        @QueryValue("dryRun") @Nullable String dryRun,
        @QueryValue("gracePeriodSeconds") @Nullable Integer gracePeriodSeconds,
        @QueryValue("ignoreStoreReadErrorWithClusterBreakingPotential") @Nullable Boolean ignoreStoreReadErrorWithClusterBreakingPotential,
        @QueryValue("orphanDependents") @Nullable Boolean orphanDependents,
        @QueryValue("propagationPolicy") @Nullable String propagationPolicy,
        @Body @Nullable V1DeleteOptions body
    );

    @Get("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects")
    @Consumes({"application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor", "application/json;stream=watch", "application/vnd.kubernetes.protobuf;stream=watch", "application/cbor-seq"})
    Mono<CustomObjectCollection> listNamespacedCustomObject(
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

    @Get("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects/{name}")
    @Consumes({"application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor"})
    Mono<CustomObject> readNamespacedCustomObject(
        @PathVariable("name") String name,
        @PathVariable("namespace") String namespace,
        @QueryValue("pretty") @Nullable String pretty
    );

    @Put("/apis/custom.test.io/v1/namespaces/{namespace}/customobjects/{name}")
    @Consumes({"application/json", "application/yaml", "application/vnd.kubernetes.protobuf", "application/cbor"})
    Mono<CustomObject> replaceNamespacedCustomObject(
        @PathVariable("name") String name,
        @PathVariable("namespace") String namespace,
        @Body CustomObject body,
        @QueryValue("pretty") @Nullable String pretty,
        @QueryValue("dryRun") @Nullable String dryRun,
        @QueryValue("fieldManager") @Nullable String fieldManager,
        @QueryValue("fieldValidation") @Nullable String fieldValidation
    );
}
