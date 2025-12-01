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

import org.jspecify.annotations.NonNull;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs in background and executes its event handler on notifications.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/util/src/main/java/io/kubernetes/client/informer/cache/ProcessorListener.java">ProcessorListener</a>
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
final class ProcessorListener<ApiType extends KubernetesObject> implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessorListener.class);

    private final AtomicBoolean active = new AtomicBoolean(false);

    private final BlockingQueue<Notification> queue = new LinkedBlockingQueue<>();

    private final ResourceEventHandler<ApiType> handler;

    // how frequently the listener wants a full resync from the shared informer
    private final long resyncPeriodInMillis;
    private OffsetDateTime nextResync;

    ProcessorListener(ResourceEventHandler<ApiType> handler, long resyncPeriodInMillis) {
        this.handler = handler;
        this.resyncPeriodInMillis = resyncPeriodInMillis;
        determineNextResync(OffsetDateTime.now());
    }

    @Override
    public void run() {
        LOG.debug("Starting processor listener");
        active.set(true);
        while (active.get()) {
            try {
                Notification notification = queue.take();
                if (notification instanceof UpdateNotification<?> updateNotification) {
                    handler.onUpdate((ApiType) updateNotification.oldObject, (ApiType) updateNotification.newObject);
                } else if (notification instanceof AddNotification<?> addNotification) {
                    handler.onAdd((ApiType) addNotification.object);
                } else if (notification instanceof DeleteNotification<?> deleteNotification) {
                    Object deletedObject = deleteNotification.object;
                    if (deletedObject instanceof DeletedFinalStateUnknown<?> deletedObjectUnknown) {
                        handler.onDelete((ApiType) deletedObjectUnknown.object(), true);
                    } else {
                        handler.onDelete((ApiType) deletedObject, false);
                    }
                } else {
                    LOG.error("Unrecognized notification: {}", notification);
                }
            } catch (InterruptedException e) {
                LOG.warn("Processor listener thread has been interrupted", e);
                active.set(false);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // Catch all exceptions here so that listeners won't quit unexpectedly
                LOG.error("Failed invoking event handler", e);
            }
        }
        LOG.debug("Stopping processor listener");
    }

    void stop() {
        active.set(false);
    }

    void add(Notification obj) {
        if (obj == null) {
            return;
        }
        queue.add(obj);
    }

    void determineNextResync(OffsetDateTime now) {
        nextResync = now.plus(Duration.ofMillis(resyncPeriodInMillis));
    }

    boolean shouldResync(OffsetDateTime now) {
        return resyncPeriodInMillis != 0 && (now.isAfter(nextResync) || now.equals(nextResync));
    }

    // visible for testing
    long getResyncPeriodInMillis() {
        return resyncPeriodInMillis;
    }

    interface Notification {
    }

    record UpdateNotification<ApiType extends KubernetesObject>(
        @NonNull ApiType oldObject,
        @NonNull ApiType newObject
    ) implements Notification {
    }

    record AddNotification<ApiType extends KubernetesObject>(
        @NonNull ApiType object
    ) implements Notification {
    }

    record DeleteNotification<ApiType extends KubernetesObject>(
        @NonNull ApiType object
    ) implements Notification {
    }
}
