package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.context.env.EmptyPropertySource;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.ConnectionString;
import io.micronaut.kubernetes.discovery.client.core.imports.KubernetesImportSupport;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import io.micronaut.kubernetes.client.openapi.model.V1SecretList;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Internal
@Singleton
final class KubernetesSecretImportSupport {

    static final String NAMESPACE = "namespace";
    static final String LABELS = "labels";
    static final String OPAQUE_SECRET_TYPE = "Opaque";

    private final CoreV1ApiReactor client;
    private final KubernetesConfiguration configuration;
    private final KubernetesLegacyImportMode legacyImportMode;

    KubernetesSecretImportSupport(CoreV1ApiReactor client,
                                  KubernetesConfiguration configuration,
                                  KubernetesLegacyImportMode legacyImportMode) {
        this.client = client;
        this.configuration = configuration;
        this.legacyImportMode = legacyImportMode;
    }

    KubernetesSecretImport newImportDeclaration(ConnectionString connectionString) {
        return resolve(new KubernetesSecretImport(connectionString.getOptions().get(NAMESPACE),
            connectionString.getPath(),
            KubernetesImportSupport.declaration(connectionString, null, KubernetesSecretPropertySourceImporter.PROVIDER).labels(),
            connectionString.isOptional()), connectionString, ConvertibleValues.empty());
    }

    KubernetesSecretImport newImportDeclaration(ConvertibleValues<Object> values) {
        return resolve(new KubernetesSecretImport(values.get(NAMESPACE, String.class).orElse(null),
            values.get("path", String.class).orElse(null),
            KubernetesImportSupport.declaration(values, null, KubernetesSecretPropertySourceImporter.PROVIDER).labels(),
            values.get("optional", Boolean.class).orElse(false)), null, values);
    }

    KubernetesSecretImport resolve(KubernetesSecretImport declaration,
                                   ConnectionString connectionString,
                                   ConvertibleValues<Object> values) {
        KubernetesImportSupport.Declaration resolved = connectionString != null
            ? KubernetesImportSupport.declaration(connectionString, configuration.getNamespace(), KubernetesSecretPropertySourceImporter.PROVIDER)
            : KubernetesImportSupport.declaration(values, configuration.getNamespace(), KubernetesSecretPropertySourceImporter.PROVIDER);
        return new KubernetesSecretImport(resolved.namespace(), resolved.name(), resolved.labels(), declaration.optional());
    }

    Optional<PropertySource> importPropertySource(PropertySourceImporter.ImportContext<KubernetesSecretImport> context) {
        legacyImportMode.registerExplicitImport(KubernetesLegacyImportMode.LegacyType.SECRET);
        KubernetesSecretImport declaration = context.importDeclaration();
        if (declaration.isExactName()) {
            return importExactName(declaration);
        }
        return importLabels(declaration);
    }

    private Optional<PropertySource> importExactName(KubernetesSecretImport declaration) {
        V1Secret secret = Flux.from(listSecrets(declaration.namespace()))
            .filter(candidate -> candidate.getMetadata() != null)
            .filter(candidate -> declaration.name().equals(candidate.getMetadata().getName()))
            .next()
            .block();
        if (secret == null) {
            return Optional.empty();
        }
        if (!OPAQUE_SECRET_TYPE.equals(secret.getType())) {
            if (declaration.optional()) {
                return Optional.empty();
            }
            throw new ConfigurationException("Config import provider [" + KubernetesSecretPropertySourceImporter.PROVIDER + "] requires Kubernetes Secret [" + declaration.name() + "] in namespace [" + declaration.namespace() + "] to be of type [" + OPAQUE_SECRET_TYPE + "]");
        }
        return toPropertySource(secret);
    }

    private Optional<PropertySource> importLabels(KubernetesSecretImport declaration) {
        String labelSelector = declaration.labels().entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((left, right) -> left + "," + right)
            .orElseThrow();
        return listSecrets(declaration.namespace(), labelSelector)
            .filter(secret -> OPAQUE_SECRET_TYPE.equals(secret.getType()))
            .concatMap(secret -> Mono.justOrEmpty(toPropertySource(secret)))
            .next()
            .blockOptional();
    }

    private Flux<V1Secret> listSecrets(String namespace) {
        return client.listNamespacedSecret(namespace, null, null, null, null, null, null, null, null, null, null, null)
            .flatMapIterable(this::secrets);
    }

    private Flux<V1Secret> listSecrets(String namespace, String labelSelector) {
        return client.listNamespacedSecret(namespace, null, null, null, null, labelSelector, null, null, null, null, null, null)
            .flatMapIterable(this::secrets);
    }

    private List<V1Secret> secrets(V1SecretList secretList) {
        return secretList.getItems() == null ? List.of() : secretList.getItems();
    }

    private static Optional<PropertySource> toPropertySource(V1Secret secret) {
        PropertySource propertySource = KubernetesConfigUtils.secretAsPropertySource(secret);
        return propertySource instanceof EmptyPropertySource ? Optional.empty() : Optional.of(propertySource);
    }

}
