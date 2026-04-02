package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.context.env.PropertySource;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.ConnectionString;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.discovery.client.core.imports.AbstractKubernetesPropertySourceImporter;
import io.micronaut.kubernetes.discovery.client.core.imports.KubernetesImportSupport;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Imports Kubernetes Secrets via {@code micronaut.config.import} using the {@code kubernetes-secret:} provider.
 *
 * @since 8.0.0
 */
public final class KubernetesSecretPropertySourceImporter extends AbstractKubernetesPropertySourceImporter<KubernetesSecretImport, KubernetesSecretImportSupport> {

    static final String PROVIDER = "kubernetes-secret";

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    protected Class<KubernetesSecretImportSupport> getSupportType() {
        return KubernetesSecretImportSupport.class;
    }

    @Override
    protected Map<String, Object> contextProperties(ImportContext<KubernetesSecretImport> context) {
        return KubernetesImportSupport.authenticationProperties(context.connectionString(), ConvertibleValues.empty(), KubernetesConfiguration.PREFIX);
    }

    @Override
    protected KubernetesSecretImport createDeclaration(KubernetesImportSupport.Declaration declaration) {
        return new KubernetesSecretImport(declaration.namespace(), declaration.name(), declaration.labels(), declaration.optional());
    }

    @Override
    protected KubernetesSecretImport resolveDeclaration(KubernetesSecretImportSupport support,
                                                        KubernetesSecretImport declaration,
                                                        @Nullable ConnectionString connectionString,
                                                        ConvertibleValues<Object> values) {
        return support.resolve(declaration, connectionString, values);
    }

    @Override
    protected Optional<PropertySource> importPropertySource(KubernetesSecretImportSupport support,
                                                            ImportContext<KubernetesSecretImport> context) {
        return support.importPropertySource(context);
    }
}
