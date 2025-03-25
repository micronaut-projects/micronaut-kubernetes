package io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter which calculates delay based on the number of failures: baseDelay*2<sup>number-of-failures</sup>.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/ratelimiter/ItemExponentialFailureRateLimiter.java">ItemExponentialFailureRateLimiter</a>
 * </p>
 */
public class ItemExponentialFailureRateLimiter<T> implements RateLimiter<T> {

    private final Duration baseDelay;
    private final Duration maxDelay;

    private final ConcurrentMap<T, AtomicLong> failures = new ConcurrentHashMap<>();

    public ItemExponentialFailureRateLimiter(Duration baseDelay, Duration maxDelay) {
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
    }

    @Override
    public Duration when(T item) {
        long exp = failures.computeIfAbsent(item, k -> new AtomicLong()).getAndIncrement();
        long d = maxDelay.toMillis() >> exp;
        return d > baseDelay.toMillis() ? baseDelay.multipliedBy(1 << exp) : maxDelay;
    }

    @Override
    public void forget(T item) {
        failures.remove(item);
    }

    @Override
    public int numRequeues(T item) {
        return (int) failures.computeIfAbsent(item, k -> new AtomicLong()).get();
    }
}
