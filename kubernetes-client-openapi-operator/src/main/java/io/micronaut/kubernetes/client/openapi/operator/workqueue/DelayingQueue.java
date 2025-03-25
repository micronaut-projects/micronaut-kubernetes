package io.micronaut.kubernetes.client.openapi.operator.workqueue;

import java.time.Duration;

/**
 * Defines a queue that can add an item at a later time. This makes it easier to
 * requeue items after failures without ending up in a hot-loop.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/DelayingQueue.java">DelayingQueue</a>
 * </p>
 */
public interface DelayingQueue<T> extends WorkQueue<T> {

    /**
     * Adds an item to the workqueue after the indicated duration has passed.
     *
     * @param item     item to add
     * @param duration specific duration
     */
    void addAfter(T item, Duration duration);
}
