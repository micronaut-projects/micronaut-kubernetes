package io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter which does a quick retry for a certain number of attempts, then a slow retry
 * after that.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/ratelimiter/ItemFastSlowRateLimiter.java">ItemFastSlowRateLimiter</a>
 * </p>
 */
public class ItemFastSlowRateLimiter<T> implements RateLimiter<T> {

    private final Duration fastDelay;
    private final Duration slowDelay;
    private final int maxFastAttempts;

    private final ConcurrentMap<T, AtomicInteger> failures = new ConcurrentHashMap<>();

    public ItemFastSlowRateLimiter(Duration fastDelay, Duration slowDelay, int maxFastAttempts) {
        this.fastDelay = fastDelay;
        this.slowDelay = slowDelay;
        this.maxFastAttempts = maxFastAttempts;
    }

    @Override
    public Duration when(T item) {
        int attempts = failures.computeIfAbsent(item, k -> new AtomicInteger()).incrementAndGet();
        return attempts <= maxFastAttempts ? fastDelay : slowDelay;
    }

    @Override
    public void forget(T item) {
        failures.remove(item);
    }

    @Override
    public int numRequeues(T item) {
        return failures.computeIfAbsent(item, k -> new AtomicInteger()).get();
    }
}
