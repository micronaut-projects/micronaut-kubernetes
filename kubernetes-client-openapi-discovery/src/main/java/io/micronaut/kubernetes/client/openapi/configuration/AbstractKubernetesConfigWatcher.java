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
package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.context.env.EmptyPropertySource;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.discovery.event.ServiceReadyEvent;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Watches for kubernetes object changes and makes appropriate changes to the {@link Environment}
 * by adding or removing {@link PropertySource}s.
 *
 * @param <T> the type of Kubernetes object to watch
 * @author Álvaro Sánchez-Mariscal
 */
abstract sealed class AbstractKubernetesConfigWatcher<T extends KubernetesObject>
    implements ResourceEventHandler<T> permits KubernetesConfigMapWatcher, KubernetesSecretWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractKubernetesConfigWatcher.class);

    // this flag controls when to start reflecting the changes to the discovery client
    final AtomicBoolean serviceStarted = new AtomicBoolean(false);

    private final Environment environment;
    private final Predicate<KubernetesObject> objectFilter;
    private final ApplicationEventPublisher<RefreshEvent> eventPublisher;

    AbstractKubernetesConfigWatcher(Environment environment,
                                    Predicate<KubernetesObject> objectFilter,
                                    ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        this.environment = environment;
        this.objectFilter = objectFilter;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    void onApplicationEvent(ServiceReadyEvent event) {
        serviceStarted.set(true);
    }

    @Override
    public void onAdd(T object) {
        if (!serviceStarted.get()) {
            return;
        }
        LOG.trace("Started processing of added kubernetes object, objectName={}, objectType={}, resourceVersion={}",
            object.getMetadata().getName(),
            object.getClass().getSimpleName(),
            object.getMetadata().getResourceVersion());
        if (objectFilter.test(object)) {
            Optional<PropertySource> propertySourceOpt = getPropertySource(object, true);
            if (propertySourceOpt.isPresent()) {
                PropertySource propertySource = propertySourceOpt.get();
                KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
                refreshEnvironment();
                LOG.trace("Added new PropertySource that was created from kubernetes object");
            }
        }
        LOG.trace("Completed processing of added kubernetes object");
    }

    @Override
    public void onUpdate(T oldObject, T newObject) {
        if (!serviceStarted.get()) {
            return;
        }
        LOG.trace("Started processing of modified kubernetes object, objectName={}, objectType={}, resourceVersion={}",
            newObject.getMetadata().getName(),
            newObject.getClass().getSimpleName(),
            newObject.getMetadata().getResourceVersion());
        if (objectFilter.test(newObject)) {
            Optional<PropertySource> propertySourceOpt = getPropertySource(newObject, true);
            if (propertySourceOpt.isPresent()) {
                PropertySource propertySource = propertySourceOpt.get();
                KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
                refreshEnvironment();
                LOG.trace("Modified existing PropertySource that was created from kubernetes object");
            }
        }
        LOG.trace("Completed processing of modified kubernetes object");
    }

    @Override
    public void onDelete(T object, boolean deletedFinalStateUnknown) {
        if (!serviceStarted.get()) {
            return;
        }
        LOG.trace("Started processing of deleted kubernetes object, objectName={}, objectType={}, resourceVersion={}",
            object.getMetadata().getName(),
            object.getClass().getSimpleName(),
            object.getMetadata().getResourceVersion());
        if (objectFilter.test(object)) {
            Optional<PropertySource> propertySourceOpt = getPropertySource(object, false);
            if (propertySourceOpt.isPresent()) {
                PropertySource propertySource = propertySourceOpt.get();
                KubernetesConfigurationClient.removePropertySourceFromCache(propertySource.getName());
                refreshEnvironment();
                LOG.trace("Removed PropertySource that was created from kubernetes object");
            }
        }
        LOG.trace("Completed processing of deleted kubernetes object");
    }

    abstract PropertySource readAsPropertySource(T object);

    private Optional<PropertySource> getPropertySource(T object, boolean checkResourceVersion) {
        if (checkResourceVersion) {
            String resourceVersion = object.getMetadata().getResourceVersion();
            String resVersionPropertyName = KubernetesConfigUtils.createResVersionPropertyName(object);
            Optional<String> resVersionPropertyValue = environment.getProperty(resVersionPropertyName, String.class);
            if (resVersionPropertyValue.isPresent() && resVersionPropertyValue.get().equals(resourceVersion)) {
                LOG.trace("Skipped kubernetes object since resource version has not been changed");
                return Optional.empty();
            }
        }
        PropertySource propertySource = readAsPropertySource(object);
        return propertySource instanceof EmptyPropertySource ? Optional.empty() : Optional.of(propertySource);
    }

    /**
     * Send a {@link RefreshEvent} when a kubernetes object change affects the {@link Environment}.
     *
     * @see io.micronaut.management.endpoint.refresh.RefreshEndpoint#refresh(Boolean)
     */
    private void refreshEnvironment() {
        final Map<String, Object> changes = environment.refreshAndDiff();
        LOG.trace("Changes in property sources: {}", changes.keySet());
        if (CollectionUtils.isNotEmpty(changes)) {
            eventPublisher.publishEvent(new RefreshEvent(changes));
        }
    }
}
