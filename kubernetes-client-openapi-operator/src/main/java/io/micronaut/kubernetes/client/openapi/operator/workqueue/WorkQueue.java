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

/**
 * The workqueue interface defines the queue behavior.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/WorkQueue.java">WorkQueue</a>
 * </p>
 */
public interface WorkQueue<T> {

    /**
     * Adds an item to the queue of items which need processing.
     *
     * @param item item to add
     */
    void add(T item);

    /**
     * Returns the current queue length, for informational purposes only.
     *
     * @return current queue length
     */
    int length();

    /**
     * Gets an item for processing. It blocks until there is an available item.
     *
     * @return the item
     */
    T get() throws InterruptedException;

    /**
     * Marks an item as completed. Also, it will add the same item back to the queue of items which
     * need processing if the item was marked as dirty while processing was in progress.
     *
     * @param item specific item
     */
    void done(T item);

    /**
     * Initiates a shutdown of the work queue. All added items whose processing not started will be ignored.
     */
    void shutdown();

    /**
     * Returns whether the queue is shutdown.
     *
     * @return {@code true} if the queue is shutdown
     */
    boolean isShutdown();
}
