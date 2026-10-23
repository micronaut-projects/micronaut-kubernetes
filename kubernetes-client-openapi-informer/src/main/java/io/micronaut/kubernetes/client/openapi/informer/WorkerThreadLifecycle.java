/*
 * Copyright 2017-2026 original authors
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

import org.jspecify.annotations.Nullable;

/**
 * Coordinates registration and interruption of an informer worker thread.
 * A stop request is permanent, including when it arrives before registration.
 */
final class WorkerThreadLifecycle {
    private volatile boolean stopped;
    private @Nullable Thread workerThread;

    /**
     * Registers the current thread unless shutdown was requested or another worker is registered.
     *
     * @return whether the current thread may begin processing
     */
    synchronized boolean registerCurrentThread() {
        if (stopped || workerThread != null) {
            return false;
        }
        workerThread = Thread.currentThread();
        return true;
    }

    boolean isStopped() {
        return stopped;
    }

    /**
     * Requests permanent shutdown and wakes the registered worker if it is blocked.
     */
    synchronized void stop() {
        stopped = true;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    /**
     * Marks the registered worker as terminated and releases its thread reference.
     */
    synchronized void finish() {
        stopped = true;
        workerThread = null;
    }
}
