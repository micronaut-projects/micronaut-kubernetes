package io.micronaut.kubernetes.client.openapi.operator.controller;

import io.micronaut.context.annotation.DefaultImplementation;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.openapi.operator.ResourceReconciler;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.RateLimitingQueue;

/**
 * The controller builder.
 */
@DefaultImplementation(DefaultControllerBuilder.class)
public interface ControllerBuilder {

    /**
     * Builds a controller.
     *
     * @param controllerConfiguration the operator's controller configuration
     * @param resourceReconciler      the operator's resource reconciler
     * @param workQueue               the operator's work queue
     * @return the default controller
     */
    @NonNull Controller build(
        @NonNull ControllerConfiguration controllerConfiguration,
        @NonNull ResourceReconciler<?> resourceReconciler,
        @NonNull RateLimitingQueue<Request> workQueue);
}
