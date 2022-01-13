/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.client.operator.controller;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.micronaut.kubernetes.client.informer.InformerConfiguration;
import io.micronaut.kubernetes.client.operator.ControllerConfiguration;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * The default implementation of {@link ControllerWatchBuilder}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Singleton
public class DefaultControllerWatchBuilder implements ControllerWatchBuilder {

    private final InformerConfiguration informerConfiguration;

    public DefaultControllerWatchBuilder(InformerConfiguration informerConfiguration) {
        this.informerConfiguration = informerConfiguration;
    }

    @NotNull
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ControllerWatch<? extends KubernetesObject> buildControllerWatch(@NotNull ControllerConfiguration operator, @NotNull WorkQueue<Request> workQueue) {
        final Predicate<? extends KubernetesObject> onAddFilter = operator.getOnAddFilter();
        final BiPredicate<? extends KubernetesObject, ? extends KubernetesObject> onUpdateFilter = operator.getOnUpdateFilter();
        final BiPredicate<? extends KubernetesObject, Boolean> onDeleteFilter = operator.getOnDeleteFilter();

        long resyncCheckPeriod = operator.getResyncCheckPeriod();
        if (resyncCheckPeriod == 0L && informerConfiguration.getResyncPeriod().isPresent()) {
            resyncCheckPeriod = informerConfiguration.getResyncPeriod().get().toMillis();
        }

        return io.kubernetes.client.extended.controller.builder.ControllerBuilder.controllerWatchBuilder(
                        operator.getApiType(),
                        workQueue)
                .withWorkQueueKeyFunc(
                        node -> new Request(node.getMetadata().getNamespace(), node.getMetadata().getName()))
                .withOnAddFilter((Predicate) onAddFilter)
                .withOnUpdateFilter(onUpdateFilter)
                .withOnDeleteFilter(onDeleteFilter)
                .withResyncPeriod(Duration.ofMillis(resyncCheckPeriod))
                .build();
    }
}
