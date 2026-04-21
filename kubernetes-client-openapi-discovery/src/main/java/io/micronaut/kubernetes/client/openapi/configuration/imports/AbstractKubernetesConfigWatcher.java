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
package io.micronaut.kubernetes.client.openapi.configuration.imports;

import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.configuration.imports.KubernetesPropertySourceImporter.ImportDeclaration;
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
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

    // this flag controls when to start reflecting the changes to the discovery client
    final AtomicBoolean serviceStarted = new AtomicBoolean(false);

    private final Environment environment;
    private final ApplicationEventPublisher<RefreshEvent> eventPublisher;

    AbstractKubernetesConfigWatcher(Environment environment,
                                    ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        this.environment = environment;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    void onApplicationEvent(ServerStartupEvent event) {
        serviceStarted.set(true);
    }

    @Override
    public void onAdd(@NonNull T object) {
        LOG.trace("Started processing of added kubernetes object, objectName={}, objectType={}, resourceVersion={}",
            object.getMetadata().getName(),
            object.getClass().getSimpleName(),
            object.getMetadata().getResourceVersion());
        boolean updated = updateCacheAndIndex(object);
        if (updated) {
            refreshEnv(object.getMetadata().getName());
        }
        LOG.trace("Completed processing of added kubernetes object");
    }

    @Override
    public void onUpdate(@NonNull T oldObject, @NonNull T newObject) {
        LOG.trace("Started processing of modified kubernetes object, objectName={}, objectType={}, resourceVersion={}",
            newObject.getMetadata().getName(),
            newObject.getClass().getSimpleName(),
            newObject.getMetadata().getResourceVersion());
        boolean updatedOld = updateCacheAndIndex(oldObject);
        boolean updatedNew = updateCacheAndIndex(newObject);
        if (updatedOld || updatedNew) {
            refreshEnv(oldObject.getMetadata().getName());
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
        boolean updated = updateCacheAndIndex(object);
        if (updated) {
            refreshEnv(object.getMetadata().getName());
        }
        LOG.trace("Completed processing of deleted kubernetes object");
    }

    private boolean updateCacheAndIndex(T object) {
        String objectName = object.getMetadata().getName();
        Map<String, String> objectLabels = object.getMetadata().getLabels();

        boolean updated = false;

        Iterator<Map.Entry<SelectorKey, ImportDeclaration>> it = WatchIndex.getIndex().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<SelectorKey, ImportDeclaration> entry = it.next();
            boolean matchByName = entry.getKey() instanceof SelectorKey.NameKey(String name)
                && objectName.equals(name);
            boolean matchByLabels = entry.getKey() instanceof SelectorKey.LabelsKey(Map<String, String> labels)
                && objectLabels.entrySet().containsAll(labels.entrySet());
            if (matchByName || matchByLabels) {
                it.remove();
                PropertySourceCache.remove(entry.getValue());
                updated = true;
            }
        }
        return updated;
    }

    private void refreshEnv(String objectName) {
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
