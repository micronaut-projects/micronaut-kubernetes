package io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter;

import java.time.Duration;
import java.util.Arrays;

/**
 * Default rate limiter for workqueue. It has both overall and per-item rate limiting.
 * The overall is a token bucket and the per-item is exponential.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/ratelimiter/DefaultControllerRateLimiter.java">DefaultControllerRateLimiter</a>
 * </p>
 */
public class DefaultControllerRateLimiter<T> implements RateLimiter<T> {

    private final RateLimiter<T> internalRateLimiter;

    public DefaultControllerRateLimiter() {
        this.internalRateLimiter =
            new MaxOfRateLimiter<>(
                Arrays.asList(
                    new ItemExponentialFailureRateLimiter<>(Duration.ofMillis(5), Duration.ofSeconds(1000)),
                    new BucketRateLimiter<>(100, 10, Duration.ofMinutes(1))));
    }

    @Override
    public Duration when(T item) {
        return internalRateLimiter.when(item);
    }

    @Override
    public void forget(T item) {
        internalRateLimiter.forget(item);
    }

    @Override
    public int numRequeues(T item) {
        return internalRateLimiter.numRequeues(item);
    }
}
