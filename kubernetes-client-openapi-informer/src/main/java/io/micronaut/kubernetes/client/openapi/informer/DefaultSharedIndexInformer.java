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

import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer;
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of informer.
 *
 * @param <ApiType> kubernetes api type
 */
final class DefaultSharedIndexInformer<ApiType extends KubernetesObject> implements SharedIndexInformer<ApiType> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSharedIndexInformer.class);

    private static final long MINIMUM_RESYNC_PERIOD_MILLIS = 1000L;

    private final Class<ApiType> apiTypeClass;

    @Nullable
    private final String namespace;

    private final InformerLogger informerLogger;

    private final DeltaFifo deltaFifo;

    private final Indexer<ApiType> indexer;

    private final ThreadFactoryUtil threadFactoryUtil;

    private final InformerWatcher<ApiType> informerWatcher;

    // how often the resync runnable should be executed
    private long resyncCheckPeriodMillis;

    // default resync period for event handlers
    private final long defaultEventHandlerResyncPeriodMillis;

    @Nullable
    private final ScheduledExecutorService resyncExecutor;

    private final SharedProcessor<ApiType> processor;

    @Nullable
    private DeltaConsumer<ApiType> deltaConsumer;

    @Nullable
    private TransformFunc transformFunc;

    private volatile boolean started = false;

    DefaultSharedIndexInformer(Class<ApiType> apiTypeClass,
                               @Nullable String namespace,
                               ThreadFactoryUtil threadFactoryUtil,
                               InformerApiCall<ApiType> informerApiCall,
                               long resyncPeriodMillis,
                               Indexer<ApiType> indexer) {
        this.apiTypeClass = apiTypeClass;
        this.namespace = namespace;
        this.informerLogger = new InformerLogger(LOG, apiTypeClass, namespace);
        this.indexer = indexer;
        deltaFifo = new DeltaFifo(indexer);
        this.threadFactoryUtil = threadFactoryUtil;
        processor = new SharedProcessor<>(getNamedThreadFactory("handler-exec"));
        informerWatcher = new InformerWatcher<>(apiTypeClass, informerApiCall, deltaFifo);
        resyncCheckPeriodMillis = resyncPeriodMillis > 0 && resyncPeriodMillis < MINIMUM_RESYNC_PERIOD_MILLIS
            ? MINIMUM_RESYNC_PERIOD_MILLIS
            : resyncPeriodMillis;
        defaultEventHandlerResyncPeriodMillis = resyncCheckPeriodMillis;
        resyncExecutor = resyncCheckPeriodMillis > 0
            ? Executors.newSingleThreadScheduledExecutor(getNamedThreadFactory("handler-resync"))
            : null;
    }

    @Override
    public void run() {
        if (started) {
            return;
        }
        started = true;
        processor.start();
        deltaConsumer = new DeltaConsumer<>(deltaFifo, indexer, processor, transformFunc);
        getNamedThreadFactory("delta-consumer").newThread(deltaConsumer).start();
        informerLogger.logInfo("Delta consumer thread started");
        if (resyncCheckPeriodMillis > 0) {
            informerLogger.logInfo("Resync job enabled, resyncCheckPeriodMillis={}", resyncCheckPeriodMillis);
            ResyncRunnable resyncRunnable = new ResyncRunnable(deltaFifo, processor::shouldResync, apiTypeClass, namespace);
            if (resyncExecutor != null) {
                resyncExecutor.scheduleAtFixedRate(resyncRunnable, resyncCheckPeriodMillis, resyncCheckPeriodMillis, TimeUnit.MILLISECONDS);
            }
        } else {
            informerLogger.logInfo("Resync job disabled");
        }
        informerWatcher.start();
    }

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        informerWatcher.stop();
        if (resyncExecutor != null) {
            resyncExecutor.shutdownNow();
        }
        if (deltaConsumer != null) {
            deltaConsumer.stop();
        }
        processor.stop();
    }

    @Override
    public Indexer<ApiType> getIndexer() {
        return indexer;
    }

    @Override
    public void addEventHandler(ResourceEventHandler<ApiType> handler) {
        addEventHandlerWithResyncPeriod(handler, defaultEventHandlerResyncPeriodMillis);
    }

    @Override
    public void addEventHandlerWithResyncPeriod(ResourceEventHandler<ApiType> handler, long resyncPeriodMillis) {
        if (started) {
            informerLogger.logWarn("Resource event handler cannot be added to already started informer");
            return;
        }
        if (resyncPeriodMillis > 0) {
            if (resyncPeriodMillis < MINIMUM_RESYNC_PERIOD_MILLIS) {
                informerLogger.logWarn(
                    "resyncPeriod={} is too small. Changing it to the minimum allowed rule of {}",
                    resyncPeriodMillis,
                    MINIMUM_RESYNC_PERIOD_MILLIS);
                resyncPeriodMillis = MINIMUM_RESYNC_PERIOD_MILLIS;
            }

            if (resyncPeriodMillis < resyncCheckPeriodMillis) {
                // if the event handler's resyncPeriod is smaller than the current resyncCheckPeriod,
                // update resyncCheckPeriod to match the event handler's resyncPeriod and adjust
                // the resync periods of all the listeners accordingly
                resyncCheckPeriodMillis = resyncPeriodMillis;
            }
        }
        processor.addListener(new ProcessorListener<>(handler, determineResyncPeriod(resyncPeriodMillis, resyncCheckPeriodMillis)));
    }

    private long determineResyncPeriod(long desired, long check) {
        if (desired == 0) {
            return desired;
        }
        if (check == 0) {
            return 0;
        }
        return Math.max(desired, check);
    }

    @Override
    public boolean hasSynced() {
        return deltaFifo.hasSynced();
    }

    @Override
    public String lastSyncResourceVersion() {
        if (!started) {
            return StringUtils.EMPTY_STRING;
        }

        return informerWatcher.getLastSyncResourceVersion();
    }

    @Override
    public void setTransform(TransformFunc transformFunc) {
        if (started) {
            throw new IllegalStateException("Cannot set transform func to a running informer");
        }
        this.transformFunc = transformFunc;
    }

    @Override
    public void resyncListeners() {
        deltaFifo.resync();
    }

    private ThreadFactory getNamedThreadFactory(String name) {
        String prefix = "informer-" + apiTypeClass.getSimpleName().toLowerCase() + "-";
        return threadFactoryUtil.getNamedThreadFactory(prefix + name + "-%d");
    }

    // visible for testing
    SharedProcessor<ApiType> getProcessor() {
        return processor;
    }
}
