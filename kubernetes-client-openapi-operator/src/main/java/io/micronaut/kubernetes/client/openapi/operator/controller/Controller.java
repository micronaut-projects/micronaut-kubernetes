package io.micronaut.kubernetes.client.openapi.operator.controller;

import io.micronaut.core.annotation.NonNull;

/**
 * The interface for operating a controller.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/controller/Controller.java">Controller</a>
 * </p>
 */
public interface Controller extends Runnable {

    /**
     * Get controller name.
     *
     * @return controller name
     */
    @NonNull String getName();

    /**
     * Shutdown the controller.
     */
    void shutdown();
}
