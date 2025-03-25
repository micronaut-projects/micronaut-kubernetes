package io.micronaut.kubernetes.client.openapi.operator.workqueue;

/**
 * The workqueue interface defines the queue behavior.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/WorkQueue.java">WorkQueue</a>
 * </p>
 */
public interface WorkQueue<T> {

    /**
     * Adds an item to the queue of items which need processing.
     *
     * @param item item to add
     */
    void add(T item);

    /**
     * Returns the current queue length, for informational purposes only.
     *
     * @return current queue length
     */
    int length();

    /**
     * Gets an item for processing. It blocks until there is an available item.
     *
     * @return the item
     */
    T get() throws InterruptedException;

    /**
     * Marks an item as completed. Also, it will add the same item back to the queue of items which
     * need processing if the item was marked as dirty while processing was in progress.
     *
     * @param item specific item
     */
    void done(T item);

    /**
     * Initiates a shutdown of the work queue. All added items whose processing not started will be ignored.
     */
    void shutdown();

    /**
     * Returns whether the queue is shutdown.
     *
     * @return {@code true} if the queue is shutdown
     */
    boolean isShutdown();
}
