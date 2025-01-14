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
package io.micronaut.kubernetes.client.openapi.informer;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
 * Manages all the registered {@link ProcessorListener} and distributes notifications.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/util/src/main/java/io/kubernetes/client/informer/cache/SharedProcessor.java">SharedProcessor</a>
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
final class SharedProcessor<ApiType extends KubernetesObject> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final List<ProcessorListener<ApiType>> listeners = new ArrayList<>();
    private final List<ProcessorListener<ApiType>> syncingListeners = new ArrayList<>();

    private final ThreadFactory threadFactory;

    SharedProcessor(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    void addListener(final ProcessorListener<ApiType> processorListener) {
        lock.writeLock().lock();
        try {
            listeners.add(processorListener);
            syncingListeners.add(processorListener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void start() {
        lock.writeLock().lock();
        try {
            for (ProcessorListener<ApiType> listener : listeners) {
                threadFactory.newThread(listener).start();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    void stop() {
        lock.writeLock().lock();
        try {
            for (ProcessorListener<ApiType> listener : listeners) {
                listener.stop();
            }
            syncingListeners.clear();
            listeners.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    void distribute(ProcessorListener.Notification notification, boolean isSync) {
        lock.readLock().lock();
        try {
            if (isSync) {
                for (ProcessorListener<ApiType> listener : syncingListeners) {
                    listener.add(notification);
                }
            } else {
                for (ProcessorListener<ApiType> listener : listeners) {
                    listener.add(notification);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    boolean shouldResync() {
        lock.writeLock().lock();
        boolean resyncNeeded = false;
        try {
            syncingListeners.clear();
            OffsetDateTime now = OffsetDateTime.now();
            for (ProcessorListener<ApiType> listener : listeners) {
                if (listener.shouldResync(now)) {
                    resyncNeeded = true;
                    syncingListeners.add(listener);
                    listener.determineNextResync(now);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        return resyncNeeded;
    }
}
