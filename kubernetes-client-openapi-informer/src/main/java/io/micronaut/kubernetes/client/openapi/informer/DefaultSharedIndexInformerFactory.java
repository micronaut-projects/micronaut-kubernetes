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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.cache.Cache;
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

/**
 * Default implementation of {@link SharedIndexInformerFactory}.
 */
@SuppressWarnings("rawtypes")
@Singleton
final class DefaultSharedIndexInformerFactory implements SharedIndexInformerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSharedIndexInformerFactory.class);

    // period value which disables resync
    private static final long DEFAULT_RESYNC_PERIOD = 0L;
    private static final boolean DEFAULT_WAIT_FOR_INITIAL_SYNC = false;

    private final InformerApiCallFactory informerApiCallFactory;
    private final InformerConfiguration informerConfiguration;
    private final ThreadFactory threadFactory;
    private final ExecutorService informerExecutor;
    private final Map<InformerKey, SharedIndexInformer> informers = new ConcurrentHashMap<>();
    private final Map<InformerKey, SharedIndexInformer> waitForInitialSyncInformers = new ConcurrentHashMap<>();

    DefaultSharedIndexInformerFactory(InformerApiCallFactory informerApiCallFactory,
                                      InformerConfiguration informerConfiguration,
                                      ThreadFactory threadFactory) {
        this.informerApiCallFactory = informerApiCallFactory;
        this.informerConfiguration = informerConfiguration;
        this.threadFactory = threadFactory;
        this.informerExecutor = Executors.newCachedThreadPool(threadFactory);
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        String namespace) {
        return sharedIndexInformerFor(apiTypeClass, namespace, null);
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        String namespace,
        String labelSelector) {
        return sharedIndexInformerFor(apiTypeClass, namespace, labelSelector, DEFAULT_WAIT_FOR_INITIAL_SYNC);
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        String namespace,
        String labelSelector,
        boolean waitForInitialSync) {
        return sharedIndexInformerFor(apiTypeClass, namespace, labelSelector, waitForInitialSync, DEFAULT_RESYNC_PERIOD);
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        String namespace,
        String labelSelector,
        boolean waitForInitialSync,
        long resyncPeriodMillis) {
        return sharedIndexInformerFor(apiTypeClass, namespace, labelSelector, waitForInitialSync, resyncPeriodMillis, null, null);
    }

    @Override
    public <ApiType extends KubernetesObject> List<SharedIndexInformer<ApiType>> sharedIndexInformersFor(
        Class<ApiType> apiTypeClass,
        List<String> namespaces,
        String labelSelector,
        boolean waitForInitialSync,
        long resyncPeriodMillis) {

        if (namespaces == null) {
            throw new IllegalArgumentException("The list of namespaces must be provided");
        }

        List<SharedIndexInformer<ApiType>> informerList = new ArrayList<>(namespaces.size());
        namespaces.forEach(namespace -> {
            if (StringUtils.isEmpty(namespace)) {
                throw new IllegalArgumentException("The namespaces list must not contain empty strings");
            }
            informerList.add(sharedIndexInformerFor(apiTypeClass, namespace, labelSelector, waitForInitialSync, resyncPeriodMillis, null, null));
        });

        return informerList;
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        String namespace,
        String labelSelector,
        boolean waitForInitialSync,
        long resyncPeriodMillis,
        Function<ApiType, String> cacheKeyFunction,
        Map<String, Function<ApiType, List<String>>> cacheIndexFunctions) {

        if (apiTypeClass == null) {
            throw new IllegalArgumentException("The apiTypeClass must be provided");
        }

        InformerKey<ApiType> informerKey = new InformerKey<>(apiTypeClass, namespace);
        if (informers.containsKey(informerKey)) {
            throw new IllegalStateException("Informer has been already created for apiTypeClass=" +
                apiTypeClass.getName() + " and namespace=" + namespace);
        }

        Indexer<ApiType> indexer = new Cache<>(cacheKeyFunction, cacheIndexFunctions);

        InformerApiCall<ApiType> informerApiCall = informerApiCallFactory.createInformerApiCall(apiTypeClass, namespace, labelSelector);

        DefaultSharedIndexInformer<ApiType> informer = new DefaultSharedIndexInformer<>(
            apiTypeClass,
            namespace,
            threadFactory,
            informerApiCall,
            resyncPeriodMillis,
            indexer);
        informers.put(informerKey, informer);
        if (waitForInitialSync) {
            waitForInitialSyncInformers.put(informerKey, informer);
        }
        return informer;
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> getExistingSharedIndexInformer(
        Class<ApiType> apiTypeClass,
        String namespace) {

        if (apiTypeClass == null) {
            throw new IllegalArgumentException("The apiTypeClass must be provided");
        }

        InformerKey<ApiType> informerKey = new InformerKey<>(apiTypeClass, namespace);
        return informers.get(informerKey);
    }

    @Override
    public void startAllRegisteredInformers() {
        informers.values().forEach(informer -> informerExecutor.submit(informer::run));

        if (waitForInitialSyncInformers.isEmpty()) {
            return;
        }

        LOG.info("Waiting for initial sync of informers: {}", waitForInitialSyncInformers.keySet());

        Duration syncTimeout = informerConfiguration.getSyncTimeout();
        Duration syncStep = informerConfiguration.getSyncStepTimeout();

        long waitLimit = System.currentTimeMillis() + syncTimeout.toMillis();
        while (waitLimit > System.currentTimeMillis()) {
            Set<InformerKey> syncedInformerKeys = new HashSet<>();
            waitForInitialSyncInformers.forEach((informerKey, informer) -> {
                if (informer.hasSynced()) {
                    syncedInformerKeys.add(informerKey);
                }
            });
            syncedInformerKeys.forEach(waitForInitialSyncInformers::remove);
            if (waitForInitialSyncInformers.isEmpty()) {
                break;
            }

            LOG.debug("Waiting {} millis to let informers to sync: {}", syncStep.toMillis(), waitForInitialSyncInformers.keySet());

            try {
                Thread.sleep(syncStep.toMillis());
            } catch (InterruptedException e) {
                LOG.warn("Active waiting for informers to sync has interrupted: {}", waitForInitialSyncInformers.keySet(), e);
                break;
            }
        }

        if (waitForInitialSyncInformers.isEmpty()) {
            LOG.info("The initial sync of informers have been successfully completed");
        } else {
            LOG.warn("These informers {} didn't sync up in the predefined time. It may happen that some kubernetes object won't be " +
                    "found in internal storages of those informers if requested before syncs get completed. Consider to raise the " +
                    "sync timeout `kubernetes.client.informer.sync-timeout` which is currently configured to {}",
                waitForInitialSyncInformers.keySet(), syncTimeout);
        }
    }

    @Override
    public void stopAllRegisteredInformers() {
        informers.values().forEach(SharedInformer::stop);
        informerExecutor.shutdownNow();
    }

    record InformerKey<ApiType extends KubernetesObject>(
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace
    ) {
    }
}
