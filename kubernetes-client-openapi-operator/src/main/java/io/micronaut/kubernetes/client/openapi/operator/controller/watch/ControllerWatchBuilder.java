package io.micronaut.kubernetes.client.openapi.operator.controller.watch;

import io.micronaut.context.annotation.DefaultImplementation;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.WorkQueue;

/**
 * The {@link ControllerWatch} builder.
 *
 * @author Pavol Gressa
 */
@DefaultImplementation(DefaultControllerWatchBuilder.class)
public interface ControllerWatchBuilder {

    /**
     * Builds {@link ControllerWatch}.
     *
     * @param controllerConfiguration the controller configuration
     * @param workQueue               the work queue
     * @return the controller watch
     */
    @NonNull
    ControllerWatch<? extends KubernetesObject> buildControllerWatch(@NonNull ControllerConfiguration controllerConfiguration, @NonNull WorkQueue<Request> workQueue);
}
