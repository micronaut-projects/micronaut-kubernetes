/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.kubernetes.configuration.imports;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.kubernetes.util.KubernetesUtils;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches for kubernetes object changes and makes appropriate changes to the {@link Environment}
 * by adding or removing {@link PropertySource}s.
 *
 * @param <T> the type of Kubernetes object to watch
 */
abstract sealed class AbstractKubernetesConfigWatcher<T extends KubernetesObject>
    implements ResourceEventHandler<T> permits KubernetesConfigMapWatcher, KubernetesSecretWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractKubernetesConfigWatcher.class);
    private static final Object REFRESH_ENV_LOCK = new Object();

    // this flag controls when to start reflecting the changes to the discovery client
    final AtomicBoolean serviceStarted = new AtomicBoolean(false);

    private final Environment environment;
    private final ApplicationEventPublisher<RefreshEvent> eventPublisher;

    AbstractKubernetesConfigWatcher(Environment environment,
                                    ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        this.environment = environment;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Marks the watcher as ready to publish refresh events after the application has started.
     *
     * @param ignoredEvent The startup event
     */
    @EventListener
    void onApplicationEvent(ServerStartupEvent ignoredEvent) {
        serviceStarted.set(true);
    }

    @Override
    public void onAdd(@NonNull T object) {
        LOG.trace("Started processing of added kubernetes object, objectName={}, objectType={}, resourceVersion={}",
            object.getMetadata().getName(),
            object.getClass().getSimpleName(),
            object.getMetadata().getResourceVersion());
        if (checkResourceVersionChanged(object)) {
            LOG.trace("Resource version of added kubernetes object has not been changed");
        } else {
            if (updateRefreshCountIfWatched(object)) {
                refreshEnv(object.getMetadata().getName());
            } else {
                LOG.trace("Added kubernetes object not used in configuration import or not watchable");
            }
        }
        LOG.trace("Completed processing of added kubernetes object");
    }

    @Override
    public void onUpdate(@NonNull T oldObject, @NonNull T newObject) {
        LOG.trace("Started processing of modified kubernetes object, objectName={}, objectType={}, resourceVersion={}",
            newObject.getMetadata().getName(),
            newObject.getClass().getSimpleName(),
            newObject.getMetadata().getResourceVersion());
        if (checkResourceVersionChanged(newObject)) {
            LOG.trace("Resource version of modified kubernetes object has not been changed");
        } else {
            if (updateRefreshCountIfWatched(oldObject) || updateRefreshCountIfWatched(newObject)) {
                refreshEnv(oldObject.getMetadata().getName());
            } else {
                LOG.trace("Modified kubernetes object not used in configuration import or not watchable");
            }
        }
        LOG.trace("Completed processing of modified kubernetes object");
    }

    @Override
    public void onDelete(@NonNull T object, boolean deletedFinalStateUnknown) {
        LOG.trace("Started processing of deleted kubernetes object, objectName={}, objectType={}, resourceVersion={}, deletedFinalStateUnknown={}",
            object.getMetadata().getName(),
            object.getClass().getSimpleName(),
            object.getMetadata().getResourceVersion(),
            deletedFinalStateUnknown);
        if (updateRefreshCountIfWatched(object)) {
            refreshEnv(object.getMetadata().getName());
        } else {
            LOG.trace("Deleted kubernetes object not used in configuration import or not watchable");
        }
        LOG.trace("Completed processing of deleted kubernetes object");
    }

    /**
     * Updates the refresh tracking state when the given kubernetes object is part of the watched
     * configuration imports.
     *
     * @param object The kubernetes object to evaluate
     * @return {@code true} if the object is watched and should trigger an environment refresh
     */
    abstract boolean updateRefreshCountIfWatched(T object);

    private boolean checkResourceVersionChanged(T object) {
        String resourceVersion = object.getMetadata().getResourceVersion();
        String resVersionPropertyName = KubernetesUtils.createResVersionPropertyName(object);
        Optional<String> resVersionPropertyValue = environment.getProperty(resVersionPropertyName, String.class);
        return resVersionPropertyValue.isPresent() && resVersionPropertyValue.get().equals(resourceVersion);
    }

    private void refreshEnv(String objectName) {
        synchronized (REFRESH_ENV_LOCK) {
            if (serviceStarted.get()) {
                LOG.trace("Starting environment refresh");
                final Map<String, Object> changes = environment.refreshAndDiff();
                LOG.trace("Completed environment refresh, changes in property sources: {}", changes.keySet());
                if (CollectionUtils.isNotEmpty(changes)) {
                    eventPublisher.publishEvent(new RefreshEvent(changes));
                }
            } else {
                LOG.warn("Skipped environment refresh, caused by changes on watched kubernetes object [{}], since the service not started yet",
                    objectName);
            }
        }
    }
}
