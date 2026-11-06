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

/**
 * Tracks the thread-safe lifecycle state of an informer worker that can start only once.
 * Shutdown is permanent, even when requested before startup. The caller is responsible
 * for waking blocked processing; this class does not start or interrupt threads.
 */
final class WorkerThreadLifecycle {
    private volatile boolean stopped;
    private boolean started;

    /**
     * Claims permission to begin processing unless the worker has already started or stopped.
     *
     * @return whether the worker may begin processing
     */
    synchronized boolean tryStart() {
        if (stopped || started) {
            return false;
        }
        started = true;
        return true;
    }

    /**
     * Reports whether shutdown has been requested or processing has finished.
     *
     * @return whether the worker should stop processing
     */
    boolean isStopped() {
        return stopped;
    }

    /**
     * Records a permanent shutdown request. Work in progress is allowed to finish;
     * the caller must wake any blocked processing separately.
     *
     * @return whether this call requested shutdown for the first time
     */
    synchronized boolean stop() {
        if (stopped) {
            return false;
        }
        stopped = true;
        return true;
    }

    /**
     * Records that processing has finished and prevents subsequent startup.
     */
    synchronized void finish() {
        stopped = true;
    }
}
