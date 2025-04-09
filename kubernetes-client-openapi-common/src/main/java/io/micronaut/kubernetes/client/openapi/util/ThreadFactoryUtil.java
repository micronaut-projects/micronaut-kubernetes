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
package io.micronaut.kubernetes.client.openapi.util;

import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory utility methods.
 */
@Internal
@Singleton
public class ThreadFactoryUtil {

    private final ThreadFactory threadFactory;

    ThreadFactoryUtil(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /**
     * Returns a thread factory which creates threads with custom names created from given name format.
     *
     * @param format the name format
     * @return thread factory
     */
    public ThreadFactory getNamedThreadFactory(String format) {
        final AtomicInteger threadNumber = new AtomicInteger(1);
        return r -> {
            Thread thread = threadFactory.newThread(r);
            thread.setName(String.format(format, threadNumber.getAndIncrement()));
            return thread;
        };
    }
}
