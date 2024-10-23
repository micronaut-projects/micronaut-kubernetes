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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;

/**
 * Watches for ConfigMap/Secret changes and makes the appropriate changes to the {@link Environment} by adding or removing
 * {@link PropertySource}s.
 *
 * @param <T> the type of Kubernetes object to watch
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public abstract class AbstractKubernetesConfigWatcher<T extends KubernetesObject> implements ResourceEventHandler<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractKubernetesConfigWatcher.class);

    private final Environment environment;
    private final ApplicationEventPublisher<RefreshEvent> eventPublisher;

    // this flag controls when to start reflecting the changes to the discovery client
    final AtomicBoolean serviceStarted = new AtomicBoolean(false);

    AbstractKubernetesConfigWatcher(Environment environment, ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        this.environment = environment;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onAdd(T config) {
        if (!serviceStarted.get()) {
            return;
        }

        PropertySource propertySource = null;
        if (config != null) {
            propertySource = readAsPropertySource(config);
        }
        if (passesIncludesExcludesLabelsFilters(config)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("PropertySource created from Config: {}", config.getMetadata().getName());
            }

            KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
            refreshEnvironment();
        }
    }

    abstract PropertySource readAsPropertySource(T config);

    @Override
    public void onUpdate(T oldConfig, T newConfig) {
        if (!serviceStarted.get()) {
            return;
        }
        PropertySource propertySource = null;
        if (newConfig != null) {
            propertySource = readAsPropertySource(newConfig);
        }
        if (passesIncludesExcludesLabelsFilters(newConfig)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("PropertySource modified by ConfigMap: {}", newConfig.getMetadata().getName());
            }

            KubernetesConfigurationClient.removePropertySourceFromCache(propertySource.getName());
            KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
            refreshEnvironment();
        }
    }

    @Override
    public void onDelete(T config, boolean deletedFinalStateUnknown) {
        if (!serviceStarted.get()) {
            return;
        }
        PropertySource propertySource = null;
        if (config != null) {
            propertySource = readAsPropertySource(config);
        }
        if (passesIncludesExcludesLabelsFilters(config)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Removed PropertySource created from ConfigMap: {}", config.getMetadata().getName());
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

    private boolean passesIncludesExcludesLabelsFilters(T config) {
        Collection<String> includes = getConfig().getIncludes();
        Collection<String> excludes = getConfig().getExcludes();

        boolean process = true;
        if (!includes.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("ConfigMap includes: {}", includes);
            }
            process = includes.contains(config.getMetadata().getName());
        } else if (!excludes.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("ConfigMap excludes: {}", excludes);
            }
            process = !excludes.contains(config.getMetadata().getName());
        }

        if (!process && LOG.isTraceEnabled()) {
            LOG.trace("ConfigMap {} not added because it doesn't match includes/excludes filters", config.getMetadata().getName());
        }

        return process;
    }

    abstract KubernetesConfiguration.AbstractConfigConfiguration getConfig();
}
