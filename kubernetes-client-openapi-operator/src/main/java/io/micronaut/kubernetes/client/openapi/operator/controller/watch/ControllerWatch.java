package io.micronaut.kubernetes.client.openapi.operator.controller.watch;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;

/**
 * The interface Controller watch defines how a controller watches certain resources.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/controller/ControllerWatch.java">ControllerWatch</a>
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
public interface ControllerWatch<ApiType extends KubernetesObject> {

    /**
     * Gets the event handler on watch events from the resource.
     *
     * @return the resource event handler
     */
    ResourceEventHandler<ApiType> getResourceEventHandler();
}
