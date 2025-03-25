package io.micronaut.kubernetes.client.openapi.operator.workqueue;

import io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter.DefaultControllerRateLimiter;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter.RateLimiter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The default rate limiting queue implementation.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/DefaultRateLimitingQueue.java">DefaultRateLimitingQueue</a>
 * </p>
 */
public class DefaultRateLimitingQueue<T> extends DefaultDelayingQueue<T> implements RateLimitingQueue<T> {

    private final RateLimiter<T> rateLimiter;

    public DefaultRateLimitingQueue(ExecutorService waitingWorker) {
        this(waitingWorker, new DefaultControllerRateLimiter<>());
    }

    public DefaultRateLimitingQueue(ExecutorService waitingWorker, RateLimiter<T> rateLimiter) {
        super(waitingWorker);
        this.rateLimiter = rateLimiter;
    }

    @Override
    public int numRequeues(T item) {
        return rateLimiter.numRequeues(item);
    }

    @Override
    public void forget(T item) {
        rateLimiter.forget(item);
    }

    @Override
    public void addRateLimited(T item) {
        addAfter(item, rateLimiter.when(item));
    }
}
