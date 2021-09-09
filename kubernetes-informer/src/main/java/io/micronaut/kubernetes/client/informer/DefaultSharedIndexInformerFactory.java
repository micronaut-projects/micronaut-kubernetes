/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.client.informer;

import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.apimachinery.GroupVersionKind;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.ListerWatcher;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.impl.DefaultSharedIndexInformer;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.Namespaces;
import io.kubernetes.client.util.Watchable;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.options.ListOptions;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.ModelMapper;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of the {@link SharedIndexInformerFactory}. The created {@link SharedIndexInformer} is shared
 * in scope of the namespace.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
@Requires(property = DefaultSharedIndexInformerFactory.INFORMER_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@Singleton
public class DefaultSharedIndexInformerFactory extends SharedInformerFactory implements SharedIndexInformerFactory {
    public static final String INFORMER_ENABLED = "kubernetes.client.informer.enabled";
    public static final Duration INFORMER_START_WAIT_TIME = Duration.ofMinutes(1);
    public static final Duration INFORMER_START_STEP_TIME = Duration.ofMillis(500);

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSharedIndexInformerFactory.class);
    private static final ModelMapper MAPPER = new ModelMapper();

    private final ApiClient apiClient;

    private final AtomicBoolean contextStarted = new AtomicBoolean(false);

    /**
     * Creates {@link DefaultSharedIndexInformer}.
     *
     * @param apiClient                 api client
     */
    public DefaultSharedIndexInformerFactory(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public <ApiType extends KubernetesObject, ApiListType extends KubernetesListObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
            Class<ApiType> apiType,
            Class<ApiListType> apiListType,
            String resourcePlural,
            String apiGroup,
            @Nullable String namespace,
            @Nullable String labelSelector,
            @Nullable Long resyncCheckPeriod,
            boolean waitForSync) {

        Objects.requireNonNull(apiType, "apiType is required to create informer");
        Objects.requireNonNull(apiListType, "apiListType is required to create informer");
        Objects.requireNonNull(resourcePlural, "resourcePlural is required to create informer");
        Objects.requireNonNull(apiGroup, "apiGroup is required to create informer");

        // use mapper to resolve the version from class name
        GroupVersionKind groupVersionKind = MAPPER.getGroupVersionKindByClass(apiType);
        String version = groupVersionKind.getVersion();

        // if namespace is null then watch all namespaces
        String ns = namespace == null ? Namespaces.NAMESPACE_ALL : namespace;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating Informer for KubernetesObject '{}' with group '{}', version '{}', resource plural '{}'," +
                    " label selector '{}' and namespace '{}'", apiType, apiGroup, version, resourcePlural, labelSelector, namespace);
        }

        final GenericKubernetesApi kubernetesApi = new GenericKubernetesApi(
                apiType,
                apiListType,
                apiGroup,
                version,
                resourcePlural,
                new CustomObjectsApi(apiClient));

        final SharedIndexInformer<ApiType> informer = sharedIndexInformerFor(
                listerWatcherFor(kubernetesApi, labelSelector, ns),
                apiType,
                ns,
                resyncCheckPeriod == null ? 0L : resyncCheckPeriod);

        if (LOG.isInfoEnabled()) {
            LOG.info("Created Informer for '{}' in namespace '{}'", apiType, ns);
        }

        if (contextStarted.get()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Informer created after the bean context started, going to publish InformerCreatedEvent");
            }
            startAllRegisteredInformers();
        }

        if (waitForSync) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Waiting for Informer<'{}'> in namespace '{}' to sync", apiType, namespace);
            }
            long waitLimit = System.currentTimeMillis() + INFORMER_START_WAIT_TIME.toMillis();

            while (waitLimit > System.currentTimeMillis()) {
                if (informer.hasSynced()) {
                    break;
                }
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Waiting {} millis to let Informer<'{}'> in namespace '{}' to sync",
                            INFORMER_START_STEP_TIME.toMillis(), apiType, namespace);
                }
                try {
                    Thread.sleep(INFORMER_START_STEP_TIME.toMillis());
                } catch (InterruptedException e) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Active waiting for the Informer<'{}'> in namespace '{}' sync up interrupted. " +
                                "Cancelling waiting.", apiType, namespace);
                    }
                    break;
                }
            }

            if (informer.hasSynced() && LOG.isInfoEnabled()) {
                LOG.info("Informer<'{}'> in namespace '{}' synced up, {} resources in the store", apiType, namespace,
                        informer.getIndexer().list().size());
            }
        }

        return informer;
    }

    @Override
    public <ApiType extends KubernetesObject, ApiListType extends KubernetesListObject> List<SharedIndexInformer<? extends KubernetesObject>> sharedIndexInformersFor(
            Class<ApiType> apiTypeClass,
            Class<ApiListType> apiListTypeClass,
            String resourcePlural,
            String apiGroup,
            @Nullable List<String> namespaces,
            @Nullable String labelSelector,
            @Nullable Long resyncCheckPeriod,
            boolean waitForSync) {

        if (namespaces == null) {
            namespaces = Collections.singletonList(Namespaces.NAMESPACE_ALL);
        }

        List<SharedIndexInformer<? extends KubernetesObject>> informers = new ArrayList<>(namespaces.size());
        for (String namespace : namespaces) {
            SharedIndexInformer<? extends KubernetesObject> informer = sharedIndexInformerFor(
                    apiTypeClass, apiListTypeClass, resourcePlural, apiGroup, namespace, labelSelector, resyncCheckPeriod, waitForSync);
            informers.add(informer);
        }
        return informers;
    }

    @Override
    public void stopAllRegisteredInformers() {
        super.stopAllRegisteredInformers(false);
    }

    @Override
    public <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> getExistingSharedIndexInformer(String namespace, Class<ApiType> apiTypeClass) {
        Type type = new NamespaceResourceClassType(namespace, apiTypeClass);
        return this.informers.getOrDefault(type, null);
    }

    @Override
    public List<SharedIndexInformer> getExistingSharedIndexInformers() {
        return new ArrayList<>(this.informers.values());
    }

    /**
     * After the context has been started the factory will automatically start the created {@link SharedIndexInformer}s.
     *
     * @param startupEvent startup event
     */
    @EventListener
    public void onStartupEvent(StartupEvent startupEvent) {
        contextStarted.set(true);
    }

    private synchronized <ApiType extends KubernetesObject, ApiListType extends KubernetesListObject> SharedIndexInformer<ApiType> sharedIndexInformerFor(
            ListerWatcher<ApiType, ApiListType> listerWatcher,
            Class<ApiType> apiTypeClass,
            String namespace,
            long resyncPeriodInMillis) {
        SharedIndexInformer<ApiType> informer =
                new DefaultSharedIndexInformer<>(apiTypeClass, listerWatcher, resyncPeriodInMillis);
        Type type = new NamespaceResourceClassType(namespace, apiTypeClass);
        this.informers.putIfAbsent(type, informer);
        return informer;
    }

    private <ApiType extends KubernetesObject, ApiListType extends KubernetesListObject>
    ListerWatcher<ApiType, ApiListType> listerWatcherFor(
            GenericKubernetesApi<ApiType, ApiListType> genericKubernetesApi, String labelSelector, String namespace) {

        return new ListerWatcher<ApiType, ApiListType>() {

            public ApiListType list(CallGeneratorParams params) throws ApiException {
                final ExtendedCallGeneratorParams generatorParams = new ExtendedCallGeneratorParams(params.watch, params.resourceVersion, params.timeoutSeconds, labelSelector);
                final ListOptions options = createListOptions(generatorParams);

                if (Namespaces.NAMESPACE_ALL.equals(namespace)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("List all namespaces with params: {}", generatorParams);
                    }
                    return genericKubernetesApi
                            .list(options)
                            .throwsApiException()
                            .getObject();
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("List namespace '{}' with params: {}", namespace, generatorParams);
                    }
                    return genericKubernetesApi
                            .list(namespace, options)
                            .throwsApiException()
                            .getObject();
                }
            }

            public Watchable<ApiType> watch(CallGeneratorParams params) throws ApiException {
                final ExtendedCallGeneratorParams generatorParams = new ExtendedCallGeneratorParams(params.watch, params.resourceVersion, params.timeoutSeconds, labelSelector);
                final ListOptions options = createListOptions(generatorParams);
                if (Namespaces.NAMESPACE_ALL.equals(namespace)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Watch all namespaces with params: {}", generatorParams);
                    }
                    return genericKubernetesApi.watch(options);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Watch namespace '{}' with params: {}", namespace, generatorParams);
                    }
                    return genericKubernetesApi.watch(namespace, options);
                }
            }
        };
    }

    private ListOptions createListOptions(ExtendedCallGeneratorParams params) {
        return new ListOptions() {
            {
                setResourceVersion(params.resourceVersion);
                setTimeoutSeconds(params.timeoutSeconds);
                setLabelSelector(params.labelSelector);
            }
        };
    }

    static class ExtendedCallGeneratorParams extends CallGeneratorParams {
        String labelSelector;

        public ExtendedCallGeneratorParams(Boolean watch, String resourceVersion, Integer timeoutSeconds, String labelSelector) {
            super(watch, resourceVersion, timeoutSeconds);
            this.labelSelector = labelSelector;
        }

        @Override
        public String toString() {
            return "ExtendedCallGeneratorParams{" +
                    "labelSelector='" + labelSelector + '\'' +
                    ", watch=" + watch +
                    ", resourceVersion='" + resourceVersion + '\'' +
                    ", timeoutSeconds=" + timeoutSeconds +
                    '}';
        }
    }

    static class NamespaceResourceClassType implements Type {
        String namespace;
        Class clazz;

        public NamespaceResourceClassType(String namespace, Class clazz) {
            this.namespace = namespace;
            this.clazz = clazz;
        }

        @Override
        public String getTypeName() {
            return namespace + "#" + TypeToken.get(clazz).getType().getTypeName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NamespaceResourceClassType that = (NamespaceResourceClassType) o;
            return namespace.equals(that.namespace) && clazz.equals(that.clazz);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, clazz);
        }
    }
}
