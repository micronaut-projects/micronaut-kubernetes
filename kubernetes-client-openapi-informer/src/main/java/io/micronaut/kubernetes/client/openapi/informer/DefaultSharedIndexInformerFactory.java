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
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementation of {@link SharedIndexInformerFactory}.
 */
@SuppressWarnings("rawtypes")
@Singleton
final class DefaultSharedIndexInformerFactory implements SharedIndexInformerFactory {

    // period value which disables resync
    private static final long DEFAULT_RESYNC_PERIOD = 0L;

    private final InformerApiCallFactory informerApiCallFactory;
    private final ThreadFactory threadFactory;
    private final ExecutorService informerExecutor;
    private final Map<InformerKey, SharedIndexInformer> informers = new ConcurrentHashMap<>();

    DefaultSharedIndexInformerFactory(InformerApiCallFactory informerApiCallFactory, ThreadFactory threadFactory) {
        this.informerApiCallFactory = informerApiCallFactory;
        this.threadFactory = threadFactory;
        this.informerExecutor = Executors.newCachedThreadPool(threadFactory);
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        String namespace) {
        return sharedIndexInformerFor(apiTypeClass, namespace, DEFAULT_RESYNC_PERIOD);
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        String namespace,
        long resyncPeriodMillis) {
        return sharedIndexInformerFor(apiTypeClass, namespace, resyncPeriodMillis, new Cache<>());
    }

    @Override
    public <ApiType extends KubernetesObject> List<SharedIndexInformer<ApiType>> sharedIndexInformersFor(
        Class<ApiType> apiTypeClass,
        List<String> namespaces,
        long resyncPeriodMillis) {

        List<SharedIndexInformer<ApiType>> informerList = new ArrayList<>(namespaces.size());
        namespaces.forEach(namespace -> {
            if (StringUtils.isEmpty(namespace)) {
                throw new IllegalArgumentException("The namespaces list must not contain empty strings");
            }
            informerList.add(sharedIndexInformerFor(apiTypeClass, namespace, resyncPeriodMillis, new Cache<>()));
        });

        return informerList;
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        String namespace,
        long resyncPeriodMillis,
        Cache<ApiType> cache) {

        InformerKey<ApiType> informerKey = new InformerKey<>(apiTypeClass, namespace);
        if (informers.containsKey(informerKey)) {
            throw new IllegalStateException("Informer has been already created for apiTypeClass=" +
                apiTypeClass + " and namespace=" + namespace);
        }

        InformerApiCall<ApiType> informerApiCall = informerApiCallFactory.createInformerApiCall(apiTypeClass, namespace);

        DefaultSharedIndexInformer<ApiType> informer = new DefaultSharedIndexInformer<>(
            apiTypeClass,
            namespace,
            threadFactory,
            informerApiCall,
            resyncPeriodMillis,
            cache);
        informers.put(informerKey, informer);
        return informer;
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> getExistingSharedIndexInformer(
        Class<ApiType> apiTypeClass,
        String namespace) {
        InformerKey<ApiType> informerKey = new InformerKey<>(apiTypeClass, namespace);
        return informers.get(informerKey);
    }

    @Override
    public void startAllRegisteredInformers() {
        informers.values().forEach(informer -> informerExecutor.submit(informer::run));
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
