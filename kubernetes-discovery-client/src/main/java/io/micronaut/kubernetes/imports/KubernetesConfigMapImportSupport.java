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
package io.micronaut.kubernetes.imports;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.ConnectionString;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.kubernetes.configuration.KubernetesLegacyImportMode;
import io.micronaut.kubernetes.configuration.KubernetesLegacyImportMode.LegacyType;
import io.micronaut.kubernetes.util.KubernetesUtils;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Internal
@Singleton
final class KubernetesConfigMapImportSupport {

    static final String NAMESPACE = "namespace";
    static final String LABELS = "labels";

    private final CoreV1ApiReactorClient client;
    private final KubernetesConfiguration configuration;
    private final KubernetesLegacyImportMode legacyImportMode;

    KubernetesConfigMapImportSupport(CoreV1ApiReactorClient client,
                                     KubernetesConfiguration configuration,
                                     KubernetesLegacyImportMode legacyImportMode) {
        this.client = client;
        this.configuration = configuration;
        this.legacyImportMode = legacyImportMode;
    }

    KubernetesConfigMapImport newImportDeclaration(ConnectionString connectionString) {
        return resolve(new KubernetesConfigMapImport(normalize(connectionString.getOptions().get(NAMESPACE)),
            normalize(connectionString.getPath()),
            parseLabels(connectionString.getOptions().get(LABELS)),
            connectionString.isOptional()), connectionString, ConvertibleValues.empty());
    }

    KubernetesConfigMapImport newImportDeclaration(ConvertibleValues<Object> values) {
        return resolve(new KubernetesConfigMapImport(normalize(values.get(NAMESPACE, String.class).orElse(null)),
            normalize(values.get("path", String.class).orElse(null)),
            parseLabels(values.get(LABELS, String.class).orElse(null)),
            values.get("optional", Boolean.class).orElse(false)), null, values);
    }

    KubernetesConfigMapImport resolve(KubernetesConfigMapImport declaration,
                                      ConnectionString connectionString,
                                      ConvertibleValues<Object> values) {
        validateSelectors(declaration.name(), declaration.labels());
        String namespace = resolveNamespace(declaration.namespace());
        return new KubernetesConfigMapImport(namespace, declaration.name(), declaration.labels(), declaration.optional());
    }

    Optional<PropertySource> importPropertySource(PropertySourceImporter.ImportContext<KubernetesConfigMapImport> context) {
        legacyImportMode.registerExplicitImport(LegacyType.CONFIG_MAP);
        KubernetesConfigMapImport declaration = context.importDeclaration();
        if (declaration.isExactName()) {
            return importExactName(context, declaration);
        }
        return importLabels(context, declaration);
    }

    private Optional<PropertySource> importExactName(PropertySourceImporter.ImportContext<KubernetesConfigMapImport> context,
                                                     KubernetesConfigMapImport declaration) {
        V1ConfigMap configMap = reactor.core.publisher.Flux.from(client.listNamespacedConfigMap(declaration.namespace()).execute())
            .flatMapIterable(configMapList -> configMapList.getItems() == null ? java.util.List.<V1ConfigMap>of() : configMapList.getItems())
            .filter(candidate -> candidate.getMetadata() != null)
            .filter(candidate -> declaration.name().equals(candidate.getMetadata().getName()))
            .next()
            .block();
        if (configMap == null) {
            return Optional.empty();
        }
        return toPropertySource(context, configMap);
    }

    private Optional<PropertySource> importLabels(PropertySourceImporter.ImportContext<KubernetesConfigMapImport> context,
                                                  KubernetesConfigMapImport declaration) {
        String labelSelector = declaration.labels().entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((left, right) -> left + "," + right)
            .orElseThrow();
        return client.listNamespacedConfigMap(declaration.namespace())
            .labelSelector(labelSelector)
            .execute()
            .flatMapIterable(configMapList -> configMapList.getItems() == null ? java.util.List.<V1ConfigMap>of() : configMapList.getItems())
            .concatMap(configMap -> reactor.core.publisher.Mono.justOrEmpty(toPropertySource(context, configMap)))
            .next()
            .blockOptional();
    }

    private static Optional<PropertySource> toPropertySource(PropertySourceImporter.ImportContext<KubernetesConfigMapImport> context,
                                                             V1ConfigMap configMap) {
        return Optional.of(KubernetesUtils.configMapAsPropertySource(configMap, context.environment().getPropertySourceLoaders()));
    }

    private static void validateSelectors(String name, Map<String, String> labels) {
        boolean hasName = StringUtils.isNotEmpty(name);
        boolean hasLabels = labels != null && !labels.isEmpty();
        if (hasName == hasLabels) {
            throw new ConfigurationException("Config import provider [" + KubernetesConfigMapPropertySourceImporter.PROVIDER + "] requires exactly one selector: resource name path or ['labels']");
        }
    }

    private String resolveNamespace(String namespace) {
        String resolved = normalize(namespace);
        if (resolved != null) {
            return resolved;
        }
        resolved = normalize(configuration.getNamespace());
        if (resolved != null) {
            return resolved;
        }
        throw new ConfigurationException("Config import provider [" + KubernetesConfigMapPropertySourceImporter.PROVIDER + "] requires a Kubernetes namespace");
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Map<String, String> parseLabels(String labelsValue) {
        String normalized = normalize(labelsValue);
        if (normalized == null) {
            return null;
        }
        Map<String, String> labels = new LinkedHashMap<>();
        for (String token : normalized.split(",")) {
            String entry = token.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int equalsIndex = entry.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex == entry.length() - 1) {
                throw new ConfigurationException("Config import provider [" + KubernetesConfigMapPropertySourceImporter.PROVIDER + "] requires labels in the form key=value");
            }
            String key = entry.substring(0, equalsIndex).trim();
            String value = entry.substring(equalsIndex + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                throw new ConfigurationException("Config import provider [" + KubernetesConfigMapPropertySourceImporter.PROVIDER + "] requires labels in the form key=value");
            }
            labels.put(key, value);
        }
        if (labels.isEmpty()) {
            throw new ConfigurationException("Config import provider [" + KubernetesConfigMapPropertySourceImporter.PROVIDER + "] requires a non-blank ['labels']");
        }
        return Map.copyOf(labels);
    }
}
