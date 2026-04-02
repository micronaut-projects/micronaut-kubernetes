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
 * Imports Kubernetes ConfigMaps via {@code micronaut.config.import} using the {@code kubernetes-configmap:} provider.
 *
 * @since 8.0.0
 */
public final class KubernetesConfigMapPropertySourceImporter extends AbstractKubernetesPropertySourceImporter<KubernetesConfigMapImport, KubernetesConfigMapImportSupport> {

    static final String PROVIDER = "kubernetes-configmap";

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    protected Class<KubernetesConfigMapImportSupport> getSupportType() {
        return KubernetesConfigMapImportSupport.class;
    }

    @Override
    protected Map<String, Object> contextProperties(ImportContext<KubernetesConfigMapImport> context) {
        return KubernetesImportSupport.authenticationProperties(context.connectionString(), ConvertibleValues.empty(), KubernetesConfiguration.PREFIX);
    }

    @Override
    protected KubernetesConfigMapImport createDeclaration(KubernetesImportSupport.Declaration declaration) {
        return new KubernetesConfigMapImport(declaration.namespace(), declaration.name(), declaration.labels(), declaration.optional());
    }

    @Override
    protected KubernetesConfigMapImport resolveDeclaration(KubernetesConfigMapImportSupport support,
                                                           KubernetesConfigMapImport declaration,
                                                           @Nullable ConnectionString connectionString,
                                                           ConvertibleValues<Object> values) {
        return support.resolve(declaration, connectionString, values);
    }

    @Override
    protected Optional<PropertySource> importPropertySource(KubernetesConfigMapImportSupport support,
                                                            ImportContext<KubernetesConfigMapImport> context) {
        return support.importPropertySource(context);
    }
}
