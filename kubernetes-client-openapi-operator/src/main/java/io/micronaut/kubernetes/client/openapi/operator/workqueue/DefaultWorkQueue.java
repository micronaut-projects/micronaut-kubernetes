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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * The default {@link WorkQueue} implementation that uses a doubly-linked list to store work items.
 * This class ensures the added work items are not in dirty set nor in currently processing set before
 * adding them to the list.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/DefaultWorkQueue.java">DefaultWorkQueue</a>
 * </p>
 *
 * @param <T> item type
 */
public class DefaultWorkQueue<T> implements WorkQueue<T> {

    // queue defines the order in which we will work on items. Every element of queue
    // should be in the dirty set and not in the processing set.
    private final LinkedList<T> queue = new LinkedList<>();

    // dirty defines all items that need to be processed.
    private final Set<T> dirty = new HashSet<>();

    // Things that are currently being processed are in the processing set.
    // These things may be simultaneously in the dirty set. When we finish
    // processing something and remove it from this set, we'll check if
    // it's in the dirty set, and if so, add it to the queue.
    private final Set<T> processing = new HashSet<>();

    private boolean shutdown = false;

    @Override
    public synchronized void add(T item) {
        if (shutdown) {
            return;
        }
        if (dirty.contains(item)) {
            return;
        }
        dirty.add(item);
        if (processing.contains(item)) {
            return;
        }
        queue.add(item);
        notify();
    }

    @Override
    public synchronized int length() {
        return queue.size();
    }

    @Override
    public synchronized T get() throws InterruptedException {
        while (queue.size() == 0 && !shutdown) {
            wait();
        }
        if (queue.size() == 0) {
            // We must be shutting down
            return null;
        }
        T obj = queue.poll();
        processing.add(obj);
        dirty.remove(obj);
        return obj;
    }

    @Override
    public synchronized void done(T item) {
        processing.remove(item);
        if (dirty.contains(item)) {
            queue.add(item);
            notify();
        }
    }

    @Override
    public synchronized void shutdown() {
        shutdown = true;
        notifyAll();
    }

    @Override
    public synchronized boolean isShutdown() {
        return shutdown;
    }
}
