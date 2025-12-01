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

import org.jspecify.annotations.NonNull;

import java.time.Duration;

/**
 * Interface for rate limiter implementations.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/ratelimiter/RateLimiter.java">RateLimiter</a>
 * </p>
 *
 * @param <T> item type
 */
public interface RateLimiter<T> {

    /**
     * Decides how long an item should wait before adding it back to the queue.
     *
     * @param item an item that should wait
     * @return how long an item should wait before adding it back to the queue
     */
    @NonNull Duration when(@NonNull T item);

    /**
     * Indicates that an item is finished being retried (failed or succeeded).
     *
     * @param item an item to remove from internal tracking
     */
    void forget(@NonNull T item);

    /**
     * Returns a number of failures that the item has had.
     *
     * @param item an item
     * @return number of failures that the item has had
     */
    int numRequeues(@NonNull T item);

    /**
     * Resets the rate limiter.
     */
    void reset();
}
