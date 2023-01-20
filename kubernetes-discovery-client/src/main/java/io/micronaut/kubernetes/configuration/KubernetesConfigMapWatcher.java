/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.kubernetes.configuration;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.discovery.event.ServiceReadyEvent;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.informer.Informer;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.kubernetes.util.KubernetesUtils;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches for ConfigMap changes and makes the appropriate changes to the {@link Environment} by adding or removing
 * {@link PropertySource}s.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Context
@Requires(env = Environment.KUBERNETES)
@Requires(beans = CoreV1ApiReactorClient.class)
@Requires(property = ConfigurationClient.ENABLED, value = "true", defaultValue = "false")
@Requires(condition = KubernetesConfigMapWatcherCondition.class)
@Informer(apiType = V1ConfigMap.class, apiListType = V1ConfigMapList.class, resourcePlural = "configmaps", apiGroup = "", labelSelectorSupplier = ConfigMapLabelSupplier.class)
public class KubernetesConfigMapWatcher implements ResourceEventHandler<V1ConfigMap> {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigMapWatcher.class);

    private final Environment environment;
    private final KubernetesConfiguration configuration;
    private final ApplicationEventPublisher<RefreshEvent> eventPublisher;

    // this flag controls when to start reflecting the changes to the discovery client
    private final AtomicBoolean serviceStarted = new AtomicBoolean(false);

    /**
     * @param environment            the {@link Environment}
     * @param apiClient              the {@link ApiClient}
     * @param coreV1Api              the {@link CoreV1Api}
     * @param coreV1ApiReactorClient the {@link CoreV1ApiReactorClient}
     * @param configuration          the {@link KubernetesConfiguration}
     * @param executorService        the IO {@link ExecutorService} where the watch publisher will be scheduled on
     * @param eventPublisher         the {@link ApplicationEventPublisher}
     * @deprecated Use new version {@link KubernetesConfigMapWatcher#KubernetesConfigMapWatcher(Environment, KubernetesConfiguration, ApplicationEventPublisher)}
     */
    public KubernetesConfigMapWatcher(Environment environment, ApiClient apiClient, CoreV1Api coreV1Api, CoreV1ApiReactorClient coreV1ApiReactorClient, KubernetesConfiguration configuration, @Named("io") ExecutorService executorService, ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        this(environment, configuration, eventPublisher);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing {}", getClass().getName());
        }
    }

    @Inject
    public KubernetesConfigMapWatcher(Environment environment, KubernetesConfiguration configuration, ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        this.environment = environment;
        this.configuration = configuration;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void onApplicationEvent(ServiceReadyEvent event) {
        serviceStarted.set(true);
    }

    @Override
    public void onAdd(V1ConfigMap configMap) {
        if (!serviceStarted.get()) {
            return;
        }

        PropertySource propertySource = null;
        if (configMap != null) {
            propertySource = KubernetesUtils.configMapAsPropertySource(configMap);
        }
        if (passesIncludesExcludesLabelsFilters(configMap)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("PropertySource created from ConfigMap: {}", configMap.getMetadata().getName());
            }

            KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
            refreshEnvironment();
        }
    }

    @Override
    public void onUpdate(V1ConfigMap oldObj, V1ConfigMap configMap) {
        if (!serviceStarted.get()) {
            return;
        }
        PropertySource propertySource = null;
        if (configMap != null) {
            propertySource = KubernetesUtils.configMapAsPropertySource(configMap);
        }
        if (passesIncludesExcludesLabelsFilters(configMap)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("PropertySource modified by ConfigMap: {}", configMap.getMetadata().getName());
            }

            KubernetesConfigurationClient.removePropertySourceFromCache(propertySource.getName());
            KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
            refreshEnvironment();
        }
    }

    @Override
    public void onDelete(V1ConfigMap configMap, boolean deletedFinalStateUnknown) {
        if (!serviceStarted.get()) {
            return;
        }
        PropertySource propertySource = null;
        if (configMap != null) {
            propertySource = KubernetesUtils.configMapAsPropertySource(configMap);
        }
        if (passesIncludesExcludesLabelsFilters(configMap)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Removed PropertySource created from ConfigMap: {}", configMap.getMetadata().getName());
            }

            KubernetesConfigurationClient.removePropertySourceFromCache(propertySource.getName());
            refreshEnvironment();
        }
    }

    /**
     * Send a {@link RefreshEvent} when a {@link V1ConfigMap} change affects the {@link Environment}.
     *
     * @see io.micronaut.management.endpoint.refresh.RefreshEndpoint#refresh(Boolean)
     */
    private void refreshEnvironment() {
        final Map<String, Object> changes = environment.refreshAndDiff();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Changes in ConfigMap property sources: [{}]", String.join(", ", changes.keySet()));
        }
        if (!changes.isEmpty()) {
            eventPublisher.publishEvent(new RefreshEvent(changes));
        }
    }

    private boolean passesIncludesExcludesLabelsFilters(V1ConfigMap configMap) {
        Collection<String> includes = configuration.getConfigMaps().getIncludes();
        Collection<String> excludes = configuration.getConfigMaps().getExcludes();

        boolean process = true;
        if (!includes.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("ConfigMap includes: {}", includes);
            }
            process = includes.contains(configMap.getMetadata().getName());
        } else if (!excludes.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("ConfigMap excludes: {}", excludes);
            }
            process = !excludes.contains(configMap.getMetadata().getName());
        }

        if (!process && LOG.isTraceEnabled()) {
            LOG.trace("ConfigMap {} not added because it doesn't match includes/excludes filters", configMap.getMetadata().getName());
        }

        return process;
    }
}
