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
import io.micronaut.kubernetes.client.openapi.informer.cache.Cache;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
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
    private final List<DefaultSharedIndexInformer> informers = new ArrayList<>();

    DefaultSharedIndexInformerFactory(InformerApiCallFactory informerApiCallFactory, ThreadFactory threadFactory) {
        this.informerApiCallFactory = informerApiCallFactory;
        this.threadFactory = threadFactory;
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
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
        Class<ApiType> apiTypeClass,
        String namespace,
        long resyncPeriodMillis,
        Cache<ApiType> cache) {

        InformerApiCall<ApiType> informerApiCall = informerApiCallFactory.createInformerApiCall(apiTypeClass, namespace);

        DefaultSharedIndexInformer<ApiType> informer = new DefaultSharedIndexInformer<>(
            apiTypeClass,
            namespace,
            threadFactory,
            informerApiCall,
            resyncPeriodMillis,
            cache);
        informers.add(informer);
        return informer;
    }

    @Override
    public void startAllRegisteredInformers() {
        informers.forEach(DefaultSharedIndexInformer::run);
    }

    @Override
    public void stopAllRegisteredInformers() {
        informers.forEach(DefaultSharedIndexInformer::stop);
    }
}
