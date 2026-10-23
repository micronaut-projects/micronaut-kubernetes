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
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * DeltaFIFO is a java portable of k/client-go's DeltaFIFO.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/util/src/main/java/io/kubernetes/client/informer/cache/DeltaFIFO.java">DeltaFIFO</a>
 * </p>
 */
final class DeltaFifo {

    private static final Logger LOG = LoggerFactory.getLogger(DeltaFifo.class);

    private final Function<KubernetesObject, String> keyFunc;

    // Groups pending deltas by resource key, preserving their order within each resource.
    // Each key appears once in queue; pop() removes the key and passes all its deltas to the consumer.
    private final Map<String, Deque<Delta>> items = new HashMap<>();

    // Maintains resource keys in FIFO order for pop().
    // Each key appears exactly once and has a corresponding non-empty deque in items.
    private final Deque<String> queue = new LinkedList<>();

    // Holds cached resources maintained by the consumer.
    private final Indexer<? extends KubernetesObject> store;

    // populated is set to true if the first batch of items inserted by Replace() has
    // been populated or Delete/Add/Update was called first
    private boolean populated = false;

    // initialPopulationCount is the number of items inserted by the first call of Replace()
    private int initialPopulationCount;

    // lock provides thread safety
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Condition notEmpty;

    DeltaFifo(Indexer<? extends KubernetesObject> store) {
        keyFunc = (Function<KubernetesObject, String>) store.getKeyFunction();
        this.store = store;
        notEmpty = lock.writeLock().newCondition();
    }

    /**
     * Adds object to the queue.
     *
     * @param deltaType the delta type
     * @param object the object
     */
    void add(DeltaType deltaType, KubernetesObject object) {
        lock.writeLock().lock();
        try {
            populated = true;
            if (deltaType == DeltaType.DELETED) {
                String id = keyOf(object);
                // Skip the "deletion" action if the object doesn't
                // exist in store and doesn't have corresponding item in items.
                if (store.getByKey(id) == null && !items.containsKey(id)) {
                    return;
                }
            }
            queueActionLocked(deltaType, object);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Replaces all objects forcibly.
     *
     * @param objects the list of objects
     */
    void replace(List<KubernetesObject> objects) {
        lock.writeLock().lock();
        try {
            Set<String> keys = new HashSet<>();
            for (KubernetesObject object : objects) {
                keys.add(keyOf(object));
                queueActionLocked(DeltaType.SYNC, object);
            }

            // Pending deltas may contain objects that have not reached the store yet.
            // Use their latest state when a relist shows that they have disappeared.
            for (var entry : List.copyOf(items.entrySet())) {
                String queuedKey = entry.getKey();
                if (keys.contains(queuedKey)) {
                    continue;
                }
                Delta latest = entry.getValue().getLast();
                if (latest.type() != DeltaType.DELETED) {
                    queueActionLocked(DeltaType.DELETED, new DeletedFinalStateUnknown<>(queuedKey, latest.object()));
                }
            }

            List<String> storedKeys = store.listKeys();
            int queueDeletion = 0;
            for (String storedKey : storedKeys) {
                if (keys.contains(storedKey) || items.containsKey(storedKey)) {
                    continue;
                }
                KubernetesObject deletedObject = store.getByKey(storedKey);
                if (deletedObject == null) {
                    LOG.warn("Key {} does not exist in known objects store, placing DeleteFinalStateUnknown marker without object", storedKey);
                }
                queueDeletion++;
                queueActionLocked(DeltaType.DELETED, new DeletedFinalStateUnknown<>(storedKey, deletedObject));
            }

            if (!populated) {
                populated = true;
                initialPopulationCount = objects.size() + queueDeletion;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Resync the queue.
     */
    void resync() {
        lock.writeLock().lock();
        try {
            List<String> keys = store.listKeys();
            for (String key : keys) {
                if (items.containsKey(key)) {
                    continue;
                }
                KubernetesObject object = store.getByKey(key);
                if (object == null) {
                    continue;
                }
                queueActionLocked(DeltaType.SYNC, object);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Pops and passes deltas to the consumer function. If there is no items in the
     * queue, the thread blocks until an item is added to the queue.
     *
     * @param func the consumer function
     * @throws InterruptedException the exception
     */
    void pop(Consumer<Deque<Delta>> func) throws InterruptedException {
        lock.writeLock().lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            String id = queue.removeFirst();
            func.accept(items.remove(id));
            if (initialPopulationCount > 0) {
                initialPopulationCount--;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns info whether the first synchronization has been completed.
     *
     * @return {@code true} if the first synchronization has been completed
     */
    boolean hasSynced() {
        lock.readLock().lock();
        try {
            return populated && initialPopulationCount == 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /** queueActionLocked appends to the delta list for the object. Caller must hold the lock. */
    private void queueActionLocked(DeltaType actionType, KubernetesObject object) {
        String id = keyOf(object);

        Deque<Delta> deltas = items.get(id);
        if (deltas == null) {
            deltas = new LinkedList<>();
        }
        deltas.add(new Delta(actionType, object));

        dedupDeltas(deltas);
        if (!items.containsKey(id)) {
            queue.add(id);
        }
        items.put(id, deltas);
        notEmpty.signalAll();
    }

    private String keyOf(KubernetesObject object) {
        if (object instanceof DeletedFinalStateUnknown<?> deletedObject) {
            return deletedObject.key();
        }
        return keyFunc.apply(object);
    }

    // re-listing and watching can deliver the same update multiple times in any
    // order. This will combine the most recent two deltas if they are the same.
    private void dedupDeltas(Deque<Delta> deltas) {
        if (deltas.size() < 2) {
            return;
        }
        Delta newest = deltas.removeLast();
        Delta previous = deltas.removeLast();
        Delta merged = dedupDeltas(newest, previous);
        if (merged != null) {
            deltas.add(merged);
        } else {
            deltas.add(previous);
            deltas.add(newest);
        }
    }

    // If given deltas represent the same event, return the delta that ought to be kept.
    private @Nullable Delta dedupDeltas(Delta newest, Delta previous) {
        Delta deletionDelta = dedupDeletionDeltas(newest, previous);
        if (deletionDelta != null) {
            return deletionDelta;
        }

        if (newest.type() == DeltaType.DELETED || previous.type() == DeltaType.DELETED) {
            return null;
        }

        var newestMetadata = newest.object().getMetadata();
        var previousMetadata = previous.object().getMetadata();

        // TODO: remove this after the cause of memory leakage is confirmed
        // Squashing deltas w/ the same resource version, note that is a temporary fix that eases memory intensity.
        if (newestMetadata != null
            && previousMetadata != null
            && Objects.equals(newestMetadata.getResourceVersion(), previousMetadata.getResourceVersion())) {
            return newest;
        }
        return null;
    }

    // Compare given deltas and if both are deletions, choose the one with more information.
    private @Nullable Delta dedupDeletionDeltas(Delta newest, Delta previous) {
        if (newest.type() != DeltaType.DELETED || previous.type() != DeltaType.DELETED) {
            return null;
        }
        return previous.object() instanceof DeletedFinalStateUnknown<?> ? newest : previous;
    }

    enum DeltaType {
        ADDED, UPDATED, DELETED, SYNC
    }
}
