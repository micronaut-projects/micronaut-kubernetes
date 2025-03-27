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

import java.time.Duration;

/**
 * Defines a queue that can add an item at a later time. This makes it easier to
 * requeue items after failures without ending up in a hot-loop.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/DelayingQueue.java">DelayingQueue</a>
 * </p>
 */
public interface DelayingQueue<T> extends WorkQueue<T> {

    /**
     * Adds an item to the workqueue after the indicated duration has passed.
     *
     * @param item     item to add
     * @param duration specific duration
     */
    void addAfter(T item, Duration duration);
}
