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

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.kubernetes.client.informer.Informer;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;

/**
 * Watches ConfigMap informer events and invalidates cached ConfigMap-backed imports.
 */
@Context
@Requires(env = Environment.KUBERNETES)
@Requires(beans = CoreV1ApiReactorClient.class)
@Requires(condition = KubernetesConfigMapWatcherCondition.class)
@Informer(apiType = V1ConfigMap.class, apiListType = V1ConfigMapList.class, resourcePlural = "configmaps", apiGroup = "")
final class KubernetesConfigMapWatcher extends AbstractKubernetesConfigWatcher<V1ConfigMap> {

    KubernetesConfigMapWatcher(Environment environment,
                               ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        super(environment, eventPublisher);
    }

    @Override
    boolean updateRefreshCountIfWatched(V1ConfigMap object) {
        return ImportDeclarationWatchIndex.updateRefreshCountIfConfigMapWatched(
            object.getMetadata().getName(),
            object.getMetadata().getLabels());
    }
}
