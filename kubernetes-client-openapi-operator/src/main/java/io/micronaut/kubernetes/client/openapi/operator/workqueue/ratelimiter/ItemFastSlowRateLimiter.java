/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 *
 * @param <T> item type
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

    @Override
    public void reset() {
        failures.clear();
    }
}
