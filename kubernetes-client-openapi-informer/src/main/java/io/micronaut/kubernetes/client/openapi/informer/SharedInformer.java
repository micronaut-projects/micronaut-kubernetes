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
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Defines basic methods of an informer.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/util/src/main/java/io/kubernetes/client/informer/SharedInformer.java">SharedInformer</a>
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
public interface SharedInformer<ApiType extends KubernetesObject> {

    /**
     * Adds event handler.
     *
     * @param handler the handler
     */
    void addEventHandler(@NonNull ResourceEventHandler<ApiType> handler);

    /**
     * Adds an event handler to the shared informer using the specified resync period.
     * Events to a single handler are delivered sequentially, but there is no
     * coordination between different handlers.
     *
     * @param handler the event handler
     * @param resyncPeriod the specific resync period
     */
    void addEventHandlerWithResyncPeriod(@NonNull ResourceEventHandler<ApiType> handler, long resyncPeriod);

    /**
     * Starts the shared informer, which won't be stopped until the stop method is called.
     */
    void run();

    /**
     * Stops the shared informer.
     */
    void stop();

    /**
     * Returns true if the shared informer's store has been synced after informer started.
     *
     * @return true if the shared informer's store has been synced after informer started
     */
    boolean hasSynced();

    /**
     * Returns the last sync resource version.
     *
     * <p>The last sync resource version is the resource version observed when last synced with
     * the underlying store. The value returned is not synchronized with access to the underlying
     * store and is not thread-safe.
     *
     * @return the last sync resource version
     */
    @Nullable
    String lastSyncResourceVersion();

    /**
     * The TransformFunc is called for each object which is about to be stored. This function is
     * intended for you to take the opportunity to remove, transform, or normalize fields. One use
     * case is to strip unused metadata fields out of objects to save on RAM cost.
     *
     * <p>Must be set before starting the informer.
     *
     * <p>Note: Since the object given to the handler may be already shared with other goroutines, it
     * is advisable to copy the object being transform before mutating it at all and returning the
     * copy to prevent data races.
     *
     * @param transformFunc the transform function
     */
    void setTransform(@NonNull TransformFunc transformFunc);

    /**
     * Resends all cached kubernetes objects to all listeners.
     */
    void resyncListeners();
}
