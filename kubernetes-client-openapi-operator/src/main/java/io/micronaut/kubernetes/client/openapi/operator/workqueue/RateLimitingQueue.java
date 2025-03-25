package io.micronaut.kubernetes.client.openapi.operator.workqueue;

/**
 * Defines a queue that rate limits items being added to the queue.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/RateLimitingQueue.java">RateLimitingQueue</a>
 * </p>
 */
public interface RateLimitingQueue<T> extends DelayingQueue<T> {

    /**
     * Adds an item to the workqueue after the rate limiter says it is ok.
     *
     * @param item item to add
     */
    void addRateLimited(T item);

    /**
     * forget indicates that an item is finished being retried. Doesn't matter whether its for perm
     * failing or for success, we'll stop the rate limiter from tracking it. This only clears the
     * `rateLimiter`, you still have to call `Done` on the queue.
     *
     * @param item item which is finished being retried
     */
    void forget(T item);

    /**
     * numRequeues returns back how many times the item was requeued.
     *
     * @param item specific item
     * @return times the item was requeued
     */
    int numRequeues(T item);
}
