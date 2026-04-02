package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.context.env.EmptyPropertySource;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.ConnectionString;
import io.micronaut.kubernetes.discovery.client.core.imports.KubernetesImportSupport;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMapList;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Internal
@Singleton
final class KubernetesConfigMapImportSupport {

    static final String NAMESPACE = "namespace";
    static final String LABELS = "labels";

    private final CoreV1ApiReactor client;
    private final KubernetesConfiguration configuration;
    private final KubernetesLegacyImportMode legacyImportMode;

    KubernetesConfigMapImportSupport(CoreV1ApiReactor client,
                                     KubernetesConfiguration configuration,
                                     KubernetesLegacyImportMode legacyImportMode) {
        this.client = client;
        this.configuration = configuration;
        this.legacyImportMode = legacyImportMode;
    }

    KubernetesConfigMapImport newImportDeclaration(ConnectionString connectionString) {
        return resolve(new KubernetesConfigMapImport(connectionString.getOptions().get(NAMESPACE),
            connectionString.getPath(),
            KubernetesImportSupport.declaration(connectionString, null, KubernetesConfigMapPropertySourceImporter.PROVIDER).labels(),
            connectionString.isOptional()), connectionString, ConvertibleValues.empty());
    }

    KubernetesConfigMapImport newImportDeclaration(ConvertibleValues<Object> values) {
        return resolve(new KubernetesConfigMapImport(values.get(NAMESPACE, String.class).orElse(null),
            values.get("path", String.class).orElse(null),
            KubernetesImportSupport.declaration(values, null, KubernetesConfigMapPropertySourceImporter.PROVIDER).labels(),
            values.get("optional", Boolean.class).orElse(false)), null, values);
    }

    KubernetesConfigMapImport resolve(KubernetesConfigMapImport declaration,
                                      ConnectionString connectionString,
                                      ConvertibleValues<Object> values) {
        KubernetesImportSupport.Declaration resolved = connectionString != null
            ? KubernetesImportSupport.declaration(connectionString, configuration.getNamespace(), KubernetesConfigMapPropertySourceImporter.PROVIDER)
            : KubernetesImportSupport.declaration(values, configuration.getNamespace(), KubernetesConfigMapPropertySourceImporter.PROVIDER);
        return new KubernetesConfigMapImport(resolved.namespace(), resolved.name(), resolved.labels(), declaration.optional());
    }

    Optional<PropertySource> importPropertySource(PropertySourceImporter.ImportContext<KubernetesConfigMapImport> context) {
        legacyImportMode.registerExplicitImport(KubernetesLegacyImportMode.LegacyType.CONFIG_MAP);
        KubernetesConfigMapImport declaration = context.importDeclaration();
        if (declaration.isExactName()) {
            return importExactName(context, declaration);
        }
        return importLabels(context, declaration);
    }

    private Optional<PropertySource> importExactName(PropertySourceImporter.ImportContext<KubernetesConfigMapImport> context,
                                                     KubernetesConfigMapImport declaration) {
        V1ConfigMap configMap = Flux.from(listConfigMaps(declaration.namespace()))
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
        return listConfigMaps(declaration.namespace(), labelSelector)
            .concatMap(configMap -> reactor.core.publisher.Mono.justOrEmpty(toPropertySource(context, configMap)))
            .next()
            .blockOptional();
    }

    private Flux<V1ConfigMap> listConfigMaps(String namespace) {
        return client.listNamespacedConfigMap(namespace, null, null, null, null, null, null, null, null, null, null, null)
            .flatMapIterable(this::configMaps);
    }

    private Flux<V1ConfigMap> listConfigMaps(String namespace, String labelSelector) {
        return client.listNamespacedConfigMap(namespace, null, null, null, null, labelSelector, null, null, null, null, null, null)
            .flatMapIterable(this::configMaps);
    }

    private List<V1ConfigMap> configMaps(V1ConfigMapList configMapList) {
        return configMapList.getItems() == null ? List.of() : configMapList.getItems();
    }

    private static Optional<PropertySource> toPropertySource(PropertySourceImporter.ImportContext<KubernetesConfigMapImport> context,
                                                             V1ConfigMap configMap) {
        PropertySource propertySource = KubernetesConfigUtils.configMapAsPropertySource(configMap, context.environment().getPropertySourceLoaders());
        return propertySource instanceof EmptyPropertySource ? Optional.empty() : Optional.of(propertySource);
    }

}
