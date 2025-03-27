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
package io.micronaut.kubernetes.client.openapi.operator.workqueue;

import io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter.DefaultControllerRateLimiter;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter.RateLimiter;

import java.util.concurrent.ExecutorService;

/**
 * The default rate limiting queue implementation.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/DefaultRateLimitingQueue.java">DefaultRateLimitingQueue</a>
 * </p>
 *
 * @param <T> item type
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
