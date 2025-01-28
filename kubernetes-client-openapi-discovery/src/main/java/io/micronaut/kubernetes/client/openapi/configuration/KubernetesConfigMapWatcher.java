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

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import io.micronaut.kubernetes.client.openapi.util.KubernetesUtils;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;

import java.util.function.Predicate;

/**
 * Watches for {@link V1ConfigMap} changes and makes the appropriate changes to
 * the {@link Environment} by adding or removing {@link PropertySource}s.
 *
 * @author Álvaro Sánchez-Mariscal
 */
@Context
@Requires(env = Environment.KUBERNETES)
@Requires(beans = CoreV1ApiReactor.class)
@Requires(property = ConfigurationClient.ENABLED, value = "true", defaultValue = "false")
@Requires(condition = KubernetesConfigMapWatcherCondition.class)
@Informer(apiType = V1ConfigMap.class, labelSelectorSupplier = KubernetesConfigMapLabelSupplier.class)
final class KubernetesConfigMapWatcher extends AbstractKubernetesConfigWatcher<V1ConfigMap> {

    KubernetesConfigMapWatcher(Environment environment,
                               KubernetesConfiguration configuration,
                               ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        super(environment, createObjectFilter(configuration), eventPublisher);
    }

    private static Predicate<KubernetesObject> createObjectFilter(KubernetesConfiguration configuration) {
        KubernetesConfiguration.KubernetesConfigMapsConfiguration configMapsConfiguration = configuration.getConfigMaps();
        Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(configMapsConfiguration.getIncludes());
        Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(configMapsConfiguration.getExcludes());
        return includesFilter.and(excludesFilter);
    }

    @Override
    PropertySource readAsPropertySource(V1ConfigMap configMap) {
        return KubernetesConfigUtils.configMapAsPropertySource(configMap);
    }
}
