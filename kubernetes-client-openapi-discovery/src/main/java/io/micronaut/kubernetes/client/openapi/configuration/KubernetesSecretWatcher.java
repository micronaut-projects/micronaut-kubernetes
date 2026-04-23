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
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import io.micronaut.kubernetes.client.openapi.util.KubernetesUtils;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;

import java.util.function.Predicate;

/**
 * Watches for {@link V1Secret} changes and makes the appropriate changes to
 * the {@link Environment} by adding or removing {@link PropertySource}s.
 *
 * @author Álvaro Sánchez-Mariscal
 * @deprecated Replaced with config import implementation
 */
@Deprecated(forRemoval = true, since = "8.0.0")
@Context
@Requires(env = Environment.KUBERNETES)
@Requires(beans = CoreV1ApiReactor.class)
@Requires(property = ConfigurationClient.ENABLED, value = "true", defaultValue = "false")
@Requires(condition = KubernetesSecretWatcherCondition.class)
@Informer(apiType = V1Secret.class, labelSelectorSupplier = KubernetesSecretLabelSupplier.class)
final class KubernetesSecretWatcher extends AbstractKubernetesConfigWatcher<V1Secret> {

    KubernetesSecretWatcher(Environment environment,
                            KubernetesConfiguration configuration,
                            ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        super(environment, createObjectFilter(configuration), eventPublisher);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate<KubernetesObject> createObjectFilter(KubernetesConfiguration configuration) {
        KubernetesConfiguration.KubernetesSecretsConfiguration secretsConfiguration = configuration.getSecrets();
        Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(secretsConfiguration.getIncludes());
        Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(secretsConfiguration.getExcludes());
        Predicate<KubernetesObject> secretTypeFilter = (Predicate) KubernetesUtils.getIncludeOpaqueSecretTypeFilter();
        return secretTypeFilter.and(includesFilter).and(excludesFilter);
    }

    @Override
    PropertySource readAsPropertySource(V1Secret secret) {
        return KubernetesConfigUtils.secretAsPropertySource(secret);
    }
}
