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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.ConnectionString;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.util.KubernetesUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link PropertySourceImporter} that resolves Micronaut configuration from Kubernetes ConfigMaps and Secrets.
 */
public final class KubernetesPropertySourceImporter implements PropertySourceImporter<ImportDeclaration> {

    public static final String KUBERNETES_CONFIG_IMPORT_CONTEXT_PROPERTY = "kubernetes-config-import-property";

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesPropertySourceImporter.class);

    private static final String PROVIDER = "kubernetes";
    private static final String CONFIG_MAP_TYPE = "config-map";
    private static final String SECRET_TYPE = "secret";

    private static final String PROVIDER_OPTION = "provider";
    private static final String TYPE_OPTION = "type";
    private static final String NAME_OPTION = "name";
    private static final String LABELS_OPTION = "labels";
    private static final String POD_LABELS_OPTION = "podLabels";
    private static final String WATCH_OPTION = "watch";
    private static final String EXCEPTION_ON_POD_LABELS_MISSING_OPTION = "exceptionOnPodLabelsMissing";
    private static final String TERMINATE_STARTUP_ON_EXCEPTION_OPTION = "terminateStartupOnException";
    private static final String OPTIONAL_OPTION = "optional";

    private static final Set<String> SUPPORTED_OPTIONS = Set.of(
        PROVIDER_OPTION,
        TYPE_OPTION,
        NAME_OPTION,
        LABELS_OPTION,
        POD_LABELS_OPTION,
        WATCH_OPTION,
        EXCEPTION_ON_POD_LABELS_MISSING_OPTION,
        TERMINATE_STARTUP_ON_EXCEPTION_OPTION,
        OPTIONAL_OPTION
    );

    private static final List<String> CONTEXT_PACKAGES = List.of(
        "io.kubernetes.client",
        "io.micronaut.aop",
        "io.micronaut.buffer",
        "io.micronaut.context",
        "io.micronaut.core",
        "io.micronaut.http",
        "io.micronaut.jackson",
        "io.micronaut.json",
        "io.micronaut.kubernetes",
        "io.micronaut.logging",
        "io.micronaut.reactor",
        "io.micronaut.retry",
        "io.micronaut.runtime",
        "io.micronaut.scheduling",
        "io.micronaut.validation",
        "io.netty",
        "jakarta.inject",
        "java.util.concurrent",
        "tools.jackson"
    );

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
        validateSupportedOptions(options.keySet());
        String name = options.get(NAME_OPTION);
        Map<String, String> labels = KubernetesUtils.parseLabels(options.get(LABELS_OPTION), PROVIDER);
        List<String> podLabels = parseList(options.get(POD_LABELS_OPTION));
        validateSelectors(name, labels, podLabels);
        boolean watch = Boolean.parseBoolean(options.getOrDefault(WATCH_OPTION, "true"));
        boolean exceptionOnPodLabelsMissing = Boolean.parseBoolean(options.get(EXCEPTION_ON_POD_LABELS_MISSING_OPTION));
        boolean terminateStartupOnException = Boolean.parseBoolean(options.get(TERMINATE_STARTUP_ON_EXCEPTION_OPTION));
        return new ImportDeclaration(type, name, labels, podLabels, watch, exceptionOnPodLabelsMissing, terminateStartupOnException);
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
        validateSupportedOptions(values.asMap().keySet());
        String type = getType(values.get(TYPE_OPTION, String.class).orElse(null));
        String name = values.get(NAME_OPTION, String.class).orElse(null);
        Map<String, String> labels = KubernetesUtils.parseLabels(values.get(LABELS_OPTION, String.class).orElse(null), PROVIDER);
        List<String> podLabels = parseList(values.get(POD_LABELS_OPTION, String.class).orElse(null));
        validateSelectors(name, labels, podLabels);
        boolean watch = values.get(WATCH_OPTION, Boolean.class).orElse(true);
        boolean exceptionOnPodLabelsMissing = values.get(EXCEPTION_ON_POD_LABELS_MISSING_OPTION, Boolean.class).orElse(false);
        boolean terminateStartupOnException = values.get(TERMINATE_STARTUP_ON_EXCEPTION_OPTION, Boolean.class).orElse(false);
        return new ImportDeclaration(type, name, labels, podLabels, watch, exceptionOnPodLabelsMissing, terminateStartupOnException);
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
        ImportDeclaration declaration = context.importDeclaration();
        if (CONFIG_MAP_TYPE.equals(declaration.type())) {
            KubernetesLegacyImportMode.registerConfigMapImport();
        } else {
            KubernetesLegacyImportMode.registerSecretImport();
        }

        ApplicationContext applicationContext = getApplicationContext(context);

        KubernetesObjectImportSupport importSupport = CONFIG_MAP_TYPE.equals(declaration.type())
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

    private String getType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new ConfigurationException("Config import provider [" + PROVIDER + "] requires 'config-map' or 'secret' type");
        }
        if (!CONFIG_MAP_TYPE.equalsIgnoreCase(type) && !SECRET_TYPE.equalsIgnoreCase(type)) {
            throw new ConfigurationException("Config import provider [" + PROVIDER + "] requires 'config-map' or 'secret' type, but type set to: '" + type + "'");
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

    private void validateSupportedOptions(Set<String> optionNames) {
        Set<String> unsupportedOptions = new LinkedHashSet<>(optionNames);
        unsupportedOptions.removeAll(SUPPORTED_OPTIONS);
        if (!unsupportedOptions.isEmpty()) {
            throw new ConfigurationException(
                "Config import provider [" + PROVIDER + "] does not support options: " + unsupportedOptions
            );
        }
    }

    private ApplicationContext getApplicationContext(@NonNull ImportContext<ImportDeclaration> importContext) {
        if (applicationContext == null) {
            LOG.debug("Creating ApplicationContext for config import");
            Environment environment = importContext.environment();
            ApplicationContextBuilder builder = ApplicationContext.builder();
            builder.beansPredicate(beanDefinition -> CONTEXT_PACKAGES.stream().anyMatch(beanDefinition.getBeanType().getName()::startsWith))
                .environments(environment.getActiveNames().toArray(String[]::new))
                .classLoader(environment.getClassLoader())
                .eventsEnabled(false)
                .eagerBeansEnabled(false)
                .deducePackage(false)
                .bootstrapEnvironment(false)
                .deduceCloudEnvironment(false)
                .configImport(false)
                .properties(Map.of(KUBERNETES_CONFIG_IMPORT_CONTEXT_PROPERTY, true));

            Collection<PropertySource> propertySources = environment.getPropertySources();
            if (CollectionUtils.isNotEmpty(propertySources)) {
                propertySources.stream()
                    .filter(ps -> ps.getName().equals("context"))
                    .findFirst()
                    .ifPresent(builder::propertySources);
            }
            applicationContext = builder.start();
            LOG.debug("Created ApplicationContext for config import");
        }
        return applicationContext;
    }
}
