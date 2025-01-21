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

import io.micronaut.core.util.CollectionUtils;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consumes delta events from {@link DeltaFifo}. It blocks if there are no events in the queue.
 *
 * @param <ApiType> kubernetes api type
 */
@SuppressWarnings("java:S2142")
final class DeltaConsumer<ApiType extends KubernetesObject> implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(DeltaConsumer.class);

    private final AtomicBoolean active = new AtomicBoolean(false);
    private final DeltaFifo deltaQueue;
    private final Indexer<ApiType> indexer;
    private final SharedProcessor<ApiType> processor;
    private final TransformFunc transformFunc;

    DeltaConsumer(DeltaFifo deltaQueue,
                  Indexer<ApiType> indexer,
                  SharedProcessor<ApiType> processor,
                  TransformFunc transformFunc) {
        this.deltaQueue = deltaQueue;
        this.indexer = indexer;
        this.processor = processor;
        this.transformFunc = transformFunc;
    }

    @Override
    public void run() {
        active.set(true);
        while (active.get()) {
            try {
                deltaQueue.pop(this::handleDeltas);
            } catch (InterruptedException t) {
                LOG.error("Delta consumer interrupted", t);
            } catch (Throwable t) {
                LOG.error("Delta consumer recovered from crashing {}", t.getMessage(), t);
            }
        }
    }

    void stop() {
        active.set(false);
    }

    void handleDeltas(Deque<AbstractMap.SimpleEntry<DeltaFifo.DeltaType, KubernetesObject>> deltas) {
        if (CollectionUtils.isEmpty(deltas)) {
            return;
        }

        deltas.forEach(delta -> {
            DeltaFifo.DeltaType deltaType = delta.getKey();
            KubernetesObject object = delta.getValue();
            ApiType transformedObject = transformFunc == null ? (ApiType) object : (ApiType) transformFunc.transform(object);
            switch (deltaType) {
                case SYNC:
                case ADDED:
                case UPDATED:
                    boolean isSync = deltaType == DeltaFifo.DeltaType.SYNC;
                    String key = indexer.getKeyFunction().apply(transformedObject);
                    ApiType oldObject = indexer.getByKey(key);
                    if (oldObject != null) {
                        indexer.update(transformedObject);
                        processor.distribute(new ProcessorListener.UpdateNotification<>(oldObject, object), isSync);
                    } else {
                        indexer.add(transformedObject);
                        processor.distribute(new ProcessorListener.AddNotification<>(object), isSync);
                    }
                    break;
                case DELETED:
                    indexer.delete(transformedObject);
                    processor.distribute(new ProcessorListener.DeleteNotification<>(object), false);
                    break;
                default:
                    break;
            }
        });
    }
}
