/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.client.operator.queue;

import io.kubernetes.client.extended.workqueue.DefaultRateLimitingQueue;
import io.kubernetes.client.extended.workqueue.RateLimitingQueue;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

import java.util.concurrent.Executors;

/**
 * The factory for {@link RateLimitingQueue}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Factory
public class RateLimitingQueueFactory {

    /**
     * Creates {@link RateLimitingQueue}.
     * @return rate limiting queue
     */
    @SuppressWarnings("rawtypes")
    @Singleton
    public RateLimitingQueue rateLimitingQueue() {
        return new DefaultRateLimitingQueue<>(Executors.newSingleThreadExecutor());
    }
}
