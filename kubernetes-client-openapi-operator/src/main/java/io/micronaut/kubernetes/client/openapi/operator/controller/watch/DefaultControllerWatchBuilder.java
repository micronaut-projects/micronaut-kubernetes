package io.micronaut.kubernetes.client.openapi.operator.controller.watch;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.WorkQueue;
import jakarta.inject.Singleton;

import java.util.function.Function;

/**
 * The default implementation of {@link ControllerWatchBuilder}.
 */
@Singleton
final class DefaultControllerWatchBuilder implements ControllerWatchBuilder {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ControllerWatch<? extends KubernetesObject> buildControllerWatch(ControllerConfiguration controllerConfiguration, WorkQueue<Request> workQueue) {
        Function<? extends KubernetesObject, Request> workKeyGenerator = node -> new Request(node.getMetadata().getNamespace(), node.getMetadata().getName());
        return new DefaultControllerWatch(
            workQueue,
            workKeyGenerator,
            controllerConfiguration.getOnAddFilter(),
            controllerConfiguration.getOnUpdateFilter(),
            controllerConfiguration.getOnDeleteFilter());
    }
}
