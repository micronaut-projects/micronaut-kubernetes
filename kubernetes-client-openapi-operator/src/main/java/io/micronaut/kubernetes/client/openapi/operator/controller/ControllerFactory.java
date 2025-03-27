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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.operator.ResourceReconciler;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.RateLimitingQueue;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Controller factory interface.
 */
public interface ControllerFactory {

    @NonNull <ApiType extends KubernetesObject> Controller createController(
        @Nullable String name,
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable Set<String> namespaces,
        @NonNull ResourceReconciler<ApiType> resourceReconciler);

    @NonNull <ApiType extends KubernetesObject> Controller createController(
        @Nullable String name,
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable Set<String> namespaces,
        @NonNull ResourceReconciler<ApiType> resourceReconciler,
        @Nullable RateLimitingQueue<Request> workQueue);

    @NonNull <ApiType extends KubernetesObject> Controller createController(
        @Nullable String name,
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable Set<String> namespaces,
        @NonNull ResourceReconciler<ApiType> resourceReconciler,
        @Nullable RateLimitingQueue<Request> workQueue,
        @Nullable Predicate<ApiType> onAddFilterPredicate,
        @Nullable BiPredicate<ApiType, ApiType> onUpdateFilterPredicate,
        @Nullable BiPredicate<ApiType, Boolean> onDeleteFilterPredicate);

    void startControllers();

    void stopControllers();
}
