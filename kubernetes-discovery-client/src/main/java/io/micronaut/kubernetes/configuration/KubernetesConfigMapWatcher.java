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

import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.util.Watch;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.event.ServiceReadyEvent;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.kubernetes.util.KubernetesUtils;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static io.micronaut.kubernetes.configuration.KubernetesConfigurationClient.KUBERNETES_CONFIG_MAP_LIST_NAME;
import static io.micronaut.kubernetes.util.KubernetesUtils.computePodLabelSelector;

/**
 * Watches for ConfigMap changes and makes the appropriate changes to the {@link Environment} by adding or removing
 * {@link PropertySource}s.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(beans = CoreV1ApiReactorClient.class)
@Requires(property = KubernetesConfiguration.PREFIX + "." + KubernetesConfiguration.KubernetesConfigMapsConfiguration.PREFIX + ".watch", notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
public class KubernetesConfigMapWatcher implements ApplicationEventListener<ServiceReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigMapWatcher.class);

    private final Environment environment;
    private final ApiClient apiClient;
    private final CoreV1Api coreV1Api;
    private final CoreV1ApiReactorClient coreV1ApiReactorClient;
    private final KubernetesConfiguration configuration;
    private final ExecutorService executorService;
    private final ApplicationEventPublisher<RefreshEvent> eventPublisher;

    /**
     * @param environment            the {@link Environment}
     * @param apiClient              the {@link ApiClient}
     * @param coreV1Api              the {@link CoreV1Api}
     * @param coreV1ApiReactorClient the {@link CoreV1ApiReactorClient}
     * @param configuration          the {@link KubernetesConfiguration}
     * @param executorService        the IO {@link ExecutorService} where the watch publisher will be scheduled on
     * @param eventPublisher         the {@link ApplicationEventPublisher}
     */
    public KubernetesConfigMapWatcher(Environment environment, ApiClient apiClient, CoreV1Api coreV1Api, CoreV1ApiReactorClient coreV1ApiReactorClient, KubernetesConfiguration configuration, @Named("io") ExecutorService executorService, ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing {}", getClass().getName());
        }

        this.environment = environment;
        this.apiClient = apiClient;
        this.coreV1Api = coreV1Api;
        this.coreV1ApiReactorClient = coreV1ApiReactorClient;
        this.configuration = configuration;
        this.executorService = executorService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onApplicationEvent(ServiceReadyEvent event) {
        executorService.execute(this::watch);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void watch() {
        while (true) {
            String lastResourceVersion = computeLastResourceVersion();
            Map<String, String> labels = configuration.getConfigMaps().getLabels();
            String labelSelector = computePodLabelSelector(coreV1ApiReactorClient, configuration.getConfigMaps().getPodLabels(), configuration.getNamespace(), labels).block();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Watching for ConfigMap events...");
            }

            Watch<V1ConfigMap> watch;
            try {
                watch = Watch.createWatch(
                        apiClient,
                        coreV1Api.listNamespacedConfigMapCall(configuration.getNamespace(), null, null, null, null, labelSelector, null, lastResourceVersion, null, null, Boolean.TRUE, null),
                        new TypeToken<Watch.Response<V1ConfigMap>>() {
                        }.getType());
            } catch (ApiException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to create the config map watch: " + e.getMessage(), e);
                }
                continue;
            }

            try {
                for (Watch.Response<V1ConfigMap> item : watch) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Received ConfigMap watch event: {}", item);
                    }
                    processEvent(item);
                }
            } finally {
                try {
                    watch.close();
                } catch (IOException e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to close the config map watch: " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    private String computeLastResourceVersion() {
        String lastResourceVersion = environment
                .getPropertySources()
                .stream()
                .filter(propertySource -> propertySource.getName().equals(KUBERNETES_CONFIG_MAP_LIST_NAME))
                .map(propertySource -> propertySource.get(KubernetesConfigurationClient.CONFIG_MAP_LIST_RESOURCE_VERSION))
                .map(o -> Long.parseLong(o.toString()))
                .max(Long::compareTo)
                .orElse(0L)
                .toString();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Latest resourceVersion is: {}", lastResourceVersion);
        }

        return lastResourceVersion;
    }

    private void processEvent(Watch.Response<V1ConfigMap> event) {

        switch (event.type) {
            case "ADDED":
                processConfigMapAdded(event.object);
                break;

            case "MODIFIED":
                processConfigMapModified(event.object);
                break;

            case "DELETED":
                processConfigMapDeleted(event.object);
                break;

            case "ERROR":
            default:
                processConfigMapErrored(event);
        }

    }

    private void processConfigMapAdded(V1ConfigMap configMap) {
        PropertySource propertySource = null;
        if (configMap != null) {
            propertySource = KubernetesUtils.configMapAsPropertySource(configMap);
        }
        if (passesIncludesExcludesLabelsFilters(configMap)) {
            KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
            refreshEnvironment();
        }
    }

    private void processConfigMapModified(V1ConfigMap configMap) {
        PropertySource propertySource = null;
        if (configMap != null) {
            propertySource = KubernetesUtils.configMapAsPropertySource(configMap);
        }
        if (passesIncludesExcludesLabelsFilters(configMap)) {
            KubernetesConfigurationClient.removePropertySourceFromCache(propertySource.getName());
            KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
            refreshEnvironment();
        }
    }

    private void processConfigMapDeleted(V1ConfigMap configMap) {
        PropertySource propertySource = null;
        if (configMap != null) {
            propertySource = KubernetesUtils.configMapAsPropertySource(configMap);
        }
        if (passesIncludesExcludesLabelsFilters(configMap)) {
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

    /**
     * Process {@code ERROR} events, unconditionally restarting the watch.
     *
     * @see <a href="https://kubernetes.io/docs/reference/using-api/api-concepts/#efficient-detection-of-changes">Efficient detection of changes</a>
     */
    private void processConfigMapErrored(Watch.Response<V1ConfigMap> event) {
        LOG.error("Kubernetes API returned an error for a ConfigMap watch event: {}", event.toString());
        KubernetesConfigurationClient.getPropertySourceCache().clear();
        refreshEnvironment();
        watch();
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
