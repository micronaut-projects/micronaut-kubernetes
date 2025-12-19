/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.client.openapi.operator.controller;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.RateLimitingQueue;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Controller factory interface.
 */
public interface ControllerFactory {

    /**
     * Creates a controller that will process events published by {@link io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer}s
     * created for given namespaces. The informers should be created before this method is called.
     *
     * @param apiTypeClass       the api type class
     * @param namespaces         the set of namespaces should be empty or set to {@code null} for cluster-wide objects (e.g. V1Node) or for
     *                           namespaced objects (e.g. V1Secret) when the controller needs to handle kubernetes objects from all namespaces
     * @param resourceReconciler the resource reconciler implementation
     * @param <ApiType>          the kubernetes api type
     * @return the created controller
     */
    @NonNull <ApiType extends KubernetesObject> Controller createController(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable Set<String> namespaces,
        @NonNull ResourceReconciler<ApiType> resourceReconciler);

    /**
     * Creates a controller that will process events published by {@link io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer}s
     * created for given namespaces. The informers should be created before this method is called.
     *
     * @param name               the name which is used to uniquely identify the created controller. If not provided, it will be generated.
     * @param apiTypeClass       the api type class
     * @param namespaces         the set of namespaces should be empty or set to {@code null} for cluster-wide objects (e.g. V1Node) or for
     *                           namespaced objects (e.g. V1Secret) when the controller needs to handle kubernetes objects from all namespaces
     * @param resourceReconciler the resource reconciler implementation
     * @param <ApiType>          the kubernetes api type
     * @return the created controller
     */
    @NonNull <ApiType extends KubernetesObject> Controller createController(
        @Nullable String name,
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable Set<String> namespaces,
        @NonNull ResourceReconciler<ApiType> resourceReconciler);

    /**
     * Creates a controller that will process events published by {@link io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer}s
     * created for given namespaces. The informers should be created before this method is called.
     *
     * @param name               the name which is used to uniquely identify the created controller. If not provided, it will be generated.
     * @param apiTypeClass       the api type class
     * @param namespaces         the set of namespaces should be empty or set to {@code null} for cluster-wide objects (e.g. V1Node) or for
     *                           namespaced objects (e.g. V1Secret) when the controller needs to handle kubernetes objects from all namespaces
     * @param resourceReconciler the resource reconciler implementation
     * @param workQueue          the producer-consumer queue where a producer is an informer and a consumer is a created controller.
     *                           If not provided, {@link io.micronaut.kubernetes.client.openapi.operator.workqueue.DefaultRateLimitingQueue} will be used.
     * @param <ApiType>          the kubernetes api type
     * @return the created controller
     */
    @NonNull <ApiType extends KubernetesObject> Controller createController(
        @Nullable String name,
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable Set<String> namespaces,
        @NonNull ResourceReconciler<ApiType> resourceReconciler,
        @Nullable RateLimitingQueue<Request> workQueue);

    /**
     * Creates a controller that will process events published by {@link io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer}s
     * created for given namespaces. The informers should be created before this method is called.
     *
     * @param name                    the name which is used to uniquely identify the created controller. If not provided, it will be generated.
     * @param apiTypeClass            the api type class
     * @param namespaces              the set of namespaces should be empty or set to {@code null} for cluster-wide objects (e.g. V1Node) or for
     *                                namespaced objects (e.g. V1Secret) when the controller needs to handle kubernetes objects from all namespaces
     * @param resourceReconciler      the resource reconciler implementation
     * @param workQueue               the producer-consumer queue where a producer is an informer and a consumer is a created controller.
     *                                If not provided, {@link io.micronaut.kubernetes.client.openapi.operator.workqueue.DefaultRateLimitingQueue} will be used.
     * @param onAddFilterPredicate    The filter which is applied by informer's resource handler when a new resource is created.
     * @param onUpdateFilterPredicate The filter which is applied by informer's resource handler when an existing resource is updated.
     * @param onDeleteFilterPredicate The filter which is applied by informer's resource handler when an existing resource is deleted.
     * @param <ApiType>               the kubernetes api type
     * @return the created controller
     */
    @NonNull <ApiType extends KubernetesObject> Controller createController(
        @Nullable String name,
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable Set<String> namespaces,
        @NonNull ResourceReconciler<ApiType> resourceReconciler,
        @Nullable RateLimitingQueue<Request> workQueue,
        @Nullable Predicate<ApiType> onAddFilterPredicate,
        @Nullable BiPredicate<ApiType, ApiType> onUpdateFilterPredicate,
        @Nullable BiPredicate<ApiType, Boolean> onDeleteFilterPredicate);

    /**
     * Starts created controllers.
     */
    void startControllers();

    /**
     * Stops created controllers.
     */
    void stopControllers();
}
