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
package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.ConnectionString;
import io.micronaut.core.util.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link PropertySourceImporter} that resolves Micronaut configuration from Kubernetes ConfigMaps and Secrets.
 */
public final class KubernetesPropertySourceImporter implements PropertySourceImporter<KubernetesPropertySourceImporter.ImportDeclaration> {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesPropertySourceImporter.class);

    private static final String PROVIDER = "kubernetes";

    private ApplicationContext applicationContext;

    @NonNull
    @Override
    public String getProvider() {
        return PROVIDER;
    }

    /**
     * Creates an import declaration from a Kubernetes config import connection string.
     *
     * @param connectionString The parsed import connection string
     * @return The normalized import declaration
     */
    @NonNull
    @Override
    public ImportDeclaration newImportDeclaration(@NonNull ConnectionString connectionString) {
        String type = getType(connectionString.getPath());
        Map<String, String> options = connectionString.getOptions();
        String namespace = options.get("namespace");
        String name = options.get("name");
        Map<String, String> labels = KubernetesConfigUtils.parseLabels(options.get("labels"), PROVIDER);
        List<String> podLabels = parseList(options.get("podLabels"));
        validateSelectors(name, labels, podLabels);
        boolean exceptionOnPodLabelsMissing = Boolean.parseBoolean(options.get("exceptionOnPodLabelsMissing"));
        boolean terminateStartupOnException = Boolean.parseBoolean(options.get("terminateStartupOnException"));
        return new ImportDeclaration(type, namespace, name, labels, podLabels, exceptionOnPodLabelsMissing, terminateStartupOnException);
    }

    /**
     * Creates an import declaration from map-like configuration values.
     *
     * @param values The raw declaration values
     * @return The normalized import declaration
     */
    @NonNull
    @Override
    public ImportDeclaration newImportDeclaration(@NonNull ConvertibleValues<Object> values) {
        String type = getType(values.get("type", String.class).orElse(null));
        String namespace = values.get("namespace", String.class).orElse(null);
        String name = values.get("name", String.class).orElse(null);
        Map<String, String> labels = KubernetesConfigUtils.parseLabels(values.get("labels", String.class).orElse(null), PROVIDER);
        List<String> podLabels = parseList(values.get("podLabels", String.class).orElse(null));
        validateSelectors(name, labels, podLabels);
        boolean exceptionOnPodLabelsMissing = values.get("exceptionOnPodLabelsMissing", Boolean.class).orElse(false);
        boolean terminateStartupOnException = values.get("terminateStartupOnException", Boolean.class).orElse(false);
        return new ImportDeclaration(type, namespace, name, labels, podLabels, exceptionOnPodLabelsMissing, terminateStartupOnException);
    }

    /**
     * Imports a property source by delegating to ConfigMap or Secret import support based on the declaration type.
     *
     * @param context The import context containing the declaration to process
     * @return The imported property source when one can be resolved
     */
    @NonNull
    @Override
    public Optional<PropertySource> importPropertySource(@NonNull ImportContext<ImportDeclaration> context) {
        ApplicationContext applicationContext = getApplicationContext();
        ImportDeclaration declaration = context.importDeclaration();
        KubernetesObjectImportSupport importSupport = "configmap".equals(declaration.type())
            ? applicationContext.findBean(KubernetesConfigMapImportSupport.class).orElse(null)
            : applicationContext.findBean(KubernetesSecretImportSupport.class).orElse(null);
        if (importSupport == null) {
            LOG.warn("Unable to find property source importer so ignoring declaration={}", declaration);
            return Optional.empty();
        }
        return importSupport.importPropertySource(context);
    }

    /**
     * Closes the lazily created application context used to resolve importer support beans.
     */
    @Override
    public void close() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
        }
    }

    /**
     * Describes a single Kubernetes config import declaration.
     *
     * @param type                        The Kubernetes resource type, either {@code configmap} or {@code secret}
     * @param namespace                   The namespace to query, or {@code null} to use the configured default
     * @param name                        The resource name to query directly
     * @param labels                      The label selector used to match resources
     * @param podLabels                   Pod label keys used to derive a selector from the current pod
     * @param exceptionOnPodLabelsMissing Whether missing pod labels should raise an exception
     * @param terminateStartupOnException Whether import failures should terminate application startup
     */
    public record ImportDeclaration(
        @NonNull String type,
        @Nullable String namespace,
        @Nullable String name,
        @Nullable Map<String, String> labels,
        @Nullable List<String> podLabels,
        boolean exceptionOnPodLabelsMissing,
        boolean terminateStartupOnException) {
    }

    private String getType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new ConfigurationException("Config import provider [" + PROVIDER + "] requires 'configmap' or 'secret' type");
        }
        if (!"configmap".equalsIgnoreCase(type) && !"secret".equalsIgnoreCase(type)) {
            throw new ConfigurationException("Config import provider [" + PROVIDER + "] requires 'configmap' or 'secret' type, but type set to: '" + type + "'");
        }
        return type.toLowerCase();
    }

    private void validateSelectors(String name, Map<String, String> labels, List<String> podLabels) {
        boolean hasName = StringUtils.isNotEmpty(name);
        boolean hasLabels = CollectionUtils.isNotEmpty(labels);
        boolean hasPodLabels = CollectionUtils.isNotEmpty(podLabels);

        if (!hasName && !hasLabels && !hasPodLabels) {
            throw new ConfigurationException(
                "Config import provider [" + PROVIDER + "] requires at least one selector: 'name', 'labels' and/or 'podLabels'"
            );
        }

        if (hasName && hasLabels) {
            throw new ConfigurationException(
                "Config import provider [" + PROVIDER + "] does not allow 'name' and 'labels' to be set at the same time"
            );
        }

        if (hasName && hasPodLabels) {
            throw new ConfigurationException(
                "Config import provider [" + PROVIDER + "] does not allow 'name' and 'podLabels' to be set at the same time"
            );
        }
    }

    private List<String> parseList(String listOption) {
        if (StringUtils.isEmpty(listOption)) {
            return null;
        }
        List<String> items = Arrays.stream(listOption.split(","))
            .map(String::trim)
            .filter(t -> !t.isEmpty())
            .toList();
        return items.isEmpty() ? null : items;
    }

    private ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            applicationContext = ApplicationContext.builder()
                .bootstrapEnvironment(false)
                .deduceCloudEnvironment(false)
                .configImport(false)
                .eagerBeansEnabled(false)
                .start();
        }
        return applicationContext;
    }
}
