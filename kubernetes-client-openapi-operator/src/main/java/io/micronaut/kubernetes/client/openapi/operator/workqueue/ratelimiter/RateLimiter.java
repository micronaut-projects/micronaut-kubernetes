package io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter;

import java.time.Duration;

/**
 * Interface for rate limiter implementations.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/ratelimiter/RateLimiter.java">RateLimiter</a>
 * </p>
 */
public interface RateLimiter<T> {

    /**
     * Decides how long an item should wait before adding it back to the queue.
     *
     * @param item an item that should wait
     */
    Duration when(T item);

    /**
     * Indicates that an item is finished being retried (failed or succeeded).
     *
     * @param item an item to remove from internal tracking
     */
    void forget(T item);

    /**
     * Returns a number of failures that the item has had
     *
     * @return number of failures that the item has had
     */
    int numRequeues(T item);
}
