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
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.WorkQueue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Resource event handler implementation which creates {@link Request} instances from informer events
 * and push it to the controller working queue.
 *
 * @param <ApiType> kubernetes api type
 */
final class ControllerResourceEventHandler<ApiType extends KubernetesObject> implements ResourceEventHandler<ApiType> {

    private final WorkQueue<Request> workQueue;
    private final Predicate<ApiType> onAddFilterPredicate;
    private final BiPredicate<ApiType, ApiType> onUpdateFilterPredicate;
    private final BiPredicate<ApiType, Boolean> onDeleteFilterPredicate;
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    ControllerResourceEventHandler(@NonNull WorkQueue<Request> workQueue,
                                   @Nullable Predicate<ApiType> onAddFilterPredicate,
                                   @Nullable BiPredicate<ApiType, ApiType> onUpdateFilterPredicate,
                                   @Nullable BiPredicate<ApiType, Boolean> onDeleteFilterPredicate) {
        this.workQueue = workQueue;
        this.onAddFilterPredicate = onAddFilterPredicate;
        this.onUpdateFilterPredicate = onUpdateFilterPredicate;
        this.onDeleteFilterPredicate = onDeleteFilterPredicate;
    }

    void enable() {
        enabled.set(true);
    }

    void disable() {
        enabled.set(false);
    }

    @Override
    public void onAdd(ApiType object) {
        if (enabled.get() && (onAddFilterPredicate == null || onAddFilterPredicate.test(object))) {
            add(object);
        }
    }

    @Override
    public void onUpdate(ApiType oldObject, ApiType newObject) {
        if (enabled.get() && (onUpdateFilterPredicate == null || onUpdateFilterPredicate.test(oldObject, newObject))) {
            add(newObject);
        }
    }

    @Override
    public void onDelete(ApiType object, boolean deletedFinalStateUnknown) {
        if (enabled.get() && (onDeleteFilterPredicate == null || onDeleteFilterPredicate.test(object, deletedFinalStateUnknown))) {
            add(object);
        }
    }

    private void add(ApiType object) {
        workQueue.add(new Request(object.getMetadata().getName(), object.getMetadata().getNamespace()));
    }
}
