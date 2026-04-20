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
package io.micronaut.kubernetes.configuration;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.kubernetes.configuration.KubernetesPropertySourceImporter.ImportDeclaration;
import io.micronaut.kubernetes.util.KubernetesUtils;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Imports Micronaut property sources from Kubernetes ConfigMaps resolved from an import declaration.
 */
@Singleton
@Requires(beans = KubernetesConfiguration.class)
final class KubernetesConfigMapImportSupport extends KubernetesObjectImportSupport {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigMapImportSupport.class);

    private final CoreV1ApiReactorClient client;
    private final KubernetesConfiguration configuration;

    KubernetesConfigMapImportSupport(CoreV1ApiReactorClient client,
                                     KubernetesConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    /**
     * Resolves ConfigMaps by explicit name or label selector and converts their contents into a property source.
     *
     * @param declaration           The import declaration describing which ConfigMap resources to resolve
     * @param propertySourceLoaders The property source loaders used to expand supported embedded formats
     * @return The imported property source when matching ConfigMap data is available
     */
    @NonNull
    @Override
    Optional<PropertySource> importPropertySource(@NonNull ImportDeclaration declaration,
                                                  @NonNull Collection<PropertySourceLoader> propertySourceLoaders) {
        String namespace = declaration.namespace() == null ? configuration.getNamespace() : declaration.namespace();
        String name = declaration.name();

        List<V1ConfigMap> configMaps;
        if (StringUtils.isNotEmpty(name)) {
            V1ConfigMap configMap = readIfExists(() -> client.readNamespacedConfigMap(name, namespace).execute().block());
            if (configMap == null) {
                LOG.debug("ConfigMap with name '{}' not found, declaration={}", name, declaration);
                return Optional.empty();
            }
            configMaps = List.of(configMap);
        } else {
            String labelSelector = KubernetesUtils.computePodLabelSelector(client, declaration.podLabels(), namespace, declaration.labels(), declaration.exceptionOnPodLabelsMissing()).block();
            if (StringUtils.isEmpty(labelSelector)) {
                LOG.debug("No label selector found for declaration {}", declaration);
                return Optional.empty();
            }
            V1ConfigMapList configMapList = client.listNamespacedConfigMap(namespace).labelSelector(labelSelector).execute().block();
            if (configMapList == null || CollectionUtils.isEmpty(configMapList.getItems())) {
                LOG.debug("ConfigMap not found for declaration={}", declaration);
                return Optional.empty();
            }
            configMaps = configMapList.getItems();
        }
        return toPropertySource(configMaps, V1ConfigMap.class, item -> KubernetesUtils.configMapAsMap(item, propertySourceLoaders));
    }
}
