package io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.SynchronizationStrategy;

import java.time.Duration;

/**
 * A light-weight token bucket implementation for RateLimiter.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/ratelimiter/BucketRateLimiter.java">BucketRateLimiter</a>
 * </p>
 */
public class BucketRateLimiter<T> implements RateLimiter<T> {
    private final Bucket bucket;

    /**
     * @param capacity                Capacity is the maximum number of tokens can be consumed.
     * @param tokensGeneratedInPeriod Tokens generated in period.
     * @param period                  Period that generating specific number of tokens.
     */
    public BucketRateLimiter(long capacity, long tokensGeneratedInPeriod, Duration period) {
        Bandwidth bandwidth = Bandwidth.builder()
            .capacity(capacity)
            .refillGreedy(tokensGeneratedInPeriod, period)
            .build();
        bucket = Bucket.builder()
            .addLimit(bandwidth)
            .withSynchronizationStrategy(SynchronizationStrategy.SYNCHRONIZED)
            .build();
    }

    @Override
    public Duration when(T item) {
        long overdraftNanos = bucket.consumeIgnoringRateLimits(1);
        return Duration.ofNanos(overdraftNanos);
    }

    @Override
    public void forget(T item) {
    }

    @Override
    public int numRequeues(T item) {
        return 0;
    }
}
