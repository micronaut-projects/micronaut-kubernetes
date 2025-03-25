package io.micronaut.kubernetes.client.openapi.operator.controller.watch;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.WorkQueue;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An event handler plumbs work-queue into a controller.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/controller/DefaultControllerWatch.java">DefaultControllerWatch</a>
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
final class DefaultControllerWatch<ApiType extends KubernetesObject> implements ControllerWatch<ApiType> {

    private final WorkQueue<Request> workQueue;
    private final Function<ApiType, Request> workKeyGenerator;

    private final Predicate<ApiType> onAddFilterPredicate;
    private final BiPredicate<ApiType, ApiType> onUpdateFilterPredicate;
    private final BiPredicate<ApiType, Boolean> onDeleteFilterPredicate;

    DefaultControllerWatch(WorkQueue<Request> workQueue,
                           Function<ApiType, Request> workKeyGenerator,
                           Predicate<ApiType> onAddFilterPredicate,
                           BiPredicate<ApiType, ApiType> onUpdateFilterPredicate,
                           BiPredicate<ApiType, Boolean> onDeleteFilterPredicate) {
        this.workQueue = workQueue;
        this.workKeyGenerator = workKeyGenerator;
        this.onAddFilterPredicate = onAddFilterPredicate;
        this.onUpdateFilterPredicate = onUpdateFilterPredicate;
        this.onDeleteFilterPredicate = onDeleteFilterPredicate;
    }

    @Override
    public ResourceEventHandler<ApiType> getResourceEventHandler() {
        return new ResourceEventHandler<>() {
            @Override
            public void onAdd(ApiType object) {
                if (onAddFilterPredicate == null || onAddFilterPredicate.test(object)) {
                    Request request = workKeyGenerator.apply(object);
                    if (request != null) {
                        workQueue.add(request);
                    }
                }
            }

            @Override
            public void onUpdate(ApiType oldObject, ApiType newObject) {
                if (onUpdateFilterPredicate == null || onUpdateFilterPredicate.test(oldObject, newObject)) {
                    Request request = workKeyGenerator.apply(newObject);
                    if (request != null) {
                        workQueue.add(request);
                    }
                }
            }

            @Override
            public void onDelete(ApiType object, boolean deletedFinalStateUnknown) {
                if (onDeleteFilterPredicate == null || onDeleteFilterPredicate.test(object, deletedFinalStateUnknown)) {
                    Request request = workKeyGenerator.apply(object);
                    if (request != null) {
                        workQueue.add(request);
                    }
                }
            }
        };
    }
}
