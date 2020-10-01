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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.event.ServiceReadyEvent;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapWatchEvent;
import io.micronaut.kubernetes.util.KubernetesUtils;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static io.micronaut.kubernetes.configuration.KubernetesConfigurationClient.KUBERNETES_CONFIG_MAP_NAME_SUFFIX;
import static io.micronaut.kubernetes.util.KubernetesUtils.computeLabelSelector;
import static io.micronaut.kubernetes.util.KubernetesUtils.computePodLabels;

/**
 * Watches for ConfigMap changes and makes the appropriate changes to the {@link Environment} by adding or removing
 * {@link PropertySource}s.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(beans = KubernetesClient.class)
@Requires(property = KubernetesConfiguration.PREFIX + "." + KubernetesConfiguration.KubernetesConfigMapsConfiguration.PREFIX + ".watch", notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
public class KubernetesConfigMapWatcher implements ApplicationEventListener<ServiceReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigMapWatcher.class);

    private Environment environment;
    private final KubernetesClient client;
    private final KubernetesConfiguration configuration;
    private final ExecutorService executorService;

    /**
     * @param environment the {@link Environment}
     * @param client the {{@link KubernetesClient}}
     * @param configuration the {@link KubernetesConfiguration}
     * @param executorService the IO {@link ExecutorService} where the watch publisher will be scheduled on
     */
    public KubernetesConfigMapWatcher(Environment environment, KubernetesClient client, KubernetesConfiguration configuration, @Named("io") ExecutorService executorService) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing {}", getClass().getName());
        }

        this.environment = environment;
        this.client = client;
        this.configuration = configuration;
        this.executorService = executorService;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onApplicationEvent(ServiceReadyEvent event) {
        long lastResourceVersion = computeLastResourceVersion();
        Map<String, String> labels = configuration.getConfigMaps().getLabels();
        Flowable<Map<String, String>> singlePodLabels = computePodLabels(client, configuration.getConfigMaps().getPodLabels(), configuration.getNamespace());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Watching for ConfigMap events...");
        }

        singlePodLabels.flatMap( podLabels -> {
            podLabels.putAll(labels);
            String labelSelector = computeLabelSelector(podLabels);
            return client.watchConfigMaps(configuration.getNamespace(), lastResourceVersion, labelSelector);
        })
                .doOnNext(e -> {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Received ConfigMap watch event: {}", e);
                    }
                })
                .doOnError(throwable -> LOG.error("Error while watching ConfigMap events", throwable))
                .onErrorReturnItem(new ConfigMapWatchEvent(ConfigMapWatchEvent.EventType.ERROR))
                .subscribeOn(Schedulers.from(this.executorService))
                .retry(5)
                .subscribe(this::processEvent);
    }

    private long computeLastResourceVersion() {
        long lastResourceVersion = this.environment
                .getPropertySources()
                .stream()
                .filter(propertySource -> propertySource.getName().endsWith(KUBERNETES_CONFIG_MAP_NAME_SUFFIX))
                .map(propertySource -> propertySource.get(KubernetesConfigurationClient.CONFIG_MAP_RESOURCE_VERSION))
                .map(o -> Long.parseLong(o.toString()))
                .max(Long::compareTo)
                .orElse(0L);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Latest resourceVersion is: {}", lastResourceVersion);
        }

        return lastResourceVersion;
    }

    private void processEvent(ConfigMapWatchEvent event) {
        switch (event.getType()) {
            case ADDED:
                processConfigMapAdded(event.getObject());
                break;

            case MODIFIED:
                processConfigMapModified(event.getObject());
                break;

            case DELETED:
                processConfigMapDeleted(event.getObject());
                break;

            case ERROR:
            default:
                processConfigMapErrored(event);
        }

    }

    private void processConfigMapAdded(ConfigMap configMap) {
        PropertySource propertySource = null;
        if (configMap != null) {
            propertySource = KubernetesUtils.configMapAsPropertySource(configMap);
        }
        if (passesIncludesExcludesLabelsFilters(configMap)) {
            this.environment.addPropertySource(propertySource);
            KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
            this.environment = environment.refresh();
        }
    }

    private void processConfigMapModified(ConfigMap configMap) {
        PropertySource propertySource = null;
        if (configMap != null) {
            propertySource = KubernetesUtils.configMapAsPropertySource(configMap);
        }
        if (passesIncludesExcludesLabelsFilters(configMap)) {
            this.environment.removePropertySource(propertySource);
            this.environment.addPropertySource(propertySource);
            KubernetesConfigurationClient.removePropertySourceFromCache(propertySource.getName());
            KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
            this.environment = environment.refresh();
        }
    }

    private void processConfigMapDeleted(ConfigMap configMap) {
        PropertySource propertySource = null;
        if (configMap != null) {
            propertySource = KubernetesUtils.configMapAsPropertySource(configMap);
        }
        if (passesIncludesExcludesLabelsFilters(configMap)) {
            this.environment.removePropertySource(propertySource);
            KubernetesConfigurationClient.removePropertySourceFromCache(propertySource.getName());
            this.environment = environment.refresh();
        }
    }

    private void processConfigMapErrored(ConfigMapWatchEvent event) {
        LOG.error("Kubernetes API returned an error for a ConfigMap watch event: {}", event.toString());
    }

    private boolean passesIncludesExcludesLabelsFilters(ConfigMap configMap) {
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
