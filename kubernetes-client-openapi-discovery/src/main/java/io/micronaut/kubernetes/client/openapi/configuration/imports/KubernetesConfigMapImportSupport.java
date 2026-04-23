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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.configuration.KubernetesConfigUtils;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMapList;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Imports Micronaut property sources from Kubernetes ConfigMaps resolved from an import declaration.
 */
@Singleton
@Requires(beans = KubernetesConfiguration.class)
final class KubernetesConfigMapImportSupport extends KubernetesObjectImportSupport {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigMapImportSupport.class);

    private final CoreV1ApiReactor client;
    private final KubernetesConfiguration configuration;

    KubernetesConfigMapImportSupport(CoreV1ApiReactor client,
                                     KubernetesConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    /**
     * Imports a ConfigMap selected by exact resource name.
     *
     * @param declaration           The import declaration
     * @param propertySourceLoaders The property source loaders used to decode structured content
     * @return The imported property source if the ConfigMap exists
     */
    @NonNull
    @Override
    Optional<PropertySource> importPropertySourceByNameSelector(@NonNull ImportDeclaration declaration,
                                                                @NonNull Collection<PropertySourceLoader> propertySourceLoaders) {
        String namespace = configuration.getNamespace();
        String name = declaration.name();

        if (declaration.watch()) {
            ImportDeclarationWatchIndex.addConfigMapNameDeclaration(name, declaration);
        }

        V1ConfigMap configMap = readIfExists(() -> client.readNamespacedConfigMap(name, namespace, null).block());
        if (configMap == null) {
            LOG.debug("ConfigMap with name '{}' not found, declaration={}", name, declaration);
            return Optional.empty();
        }

        return toPropertySource(List.of(configMap), V1ConfigMap.class, item -> KubernetesConfigUtils.configMapAsMap(item, propertySourceLoaders));
    }

    /**
     * Imports ConfigMaps selected by labels or by labels derived from the running pod.
     *
     * @param declaration           The import declaration
     * @param propertySourceLoaders The property source loaders used to decode structured content
     * @return The imported property source if any matching ConfigMaps exist
     */
    @NonNull
    @Override
    Optional<PropertySource> importPropertySourceByLabelsSelector(@NonNull ImportDeclaration declaration,
                                                                  @NonNull Collection<PropertySourceLoader> propertySourceLoaders) {
        String namespace = configuration.getNamespace();
        Map<String, String> labels = KubernetesConfigUtils.computePodLabels(client, declaration.podLabels(), namespace, declaration.labels(), declaration.exceptionOnPodLabelsMissing()).block();

        if (declaration.watch()) {
            ImportDeclarationWatchIndex.addConfigMapLabelsDeclaration(labels, declaration);
        }

        String labelSelector = KubernetesConfigUtils.computeLabelSelector(labels);
        if (StringUtils.isEmpty(labelSelector)) {
            LOG.debug("No label selector found for declaration {}", declaration);
            return Optional.empty();
        }

        V1ConfigMapList configMapList = client.listNamespacedConfigMap(namespace, null, null, null, null, labelSelector, null, null, null, null, null, null).block();
        if (configMapList == null || CollectionUtils.isEmpty(configMapList.getItems())) {
            LOG.debug("ConfigMap not found for declaration={}", declaration);
            return Optional.empty();
        }

        return toPropertySource(configMapList.getItems(), V1ConfigMap.class, item -> KubernetesConfigUtils.configMapAsMap(item, propertySourceLoaders));
    }
}
