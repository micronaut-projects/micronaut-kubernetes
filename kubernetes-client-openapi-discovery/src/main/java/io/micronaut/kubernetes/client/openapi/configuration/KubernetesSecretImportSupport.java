package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.configuration.KubernetesPropertySourceImporter.ImportDeclaration;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import io.micronaut.kubernetes.client.openapi.model.V1SecretList;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
@Requires(beans = KubernetesConfiguration.class)
final class KubernetesSecretImportSupport extends KubernetesObjectImportSupport {

    private final CoreV1ApiReactor client;
    private final KubernetesConfiguration configuration;

    KubernetesSecretImportSupport(CoreV1ApiReactor client,
                                  KubernetesConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    @NonNull
    @Override
    Optional<PropertySource> importPropertySource(@NonNull ImportDeclaration declaration,
                                                  @NonNull Collection<PropertySourceLoader> propertySourceLoaders) {
        KubernetesLegacyImportMode.registerSecretImport();

        String namespace = declaration.namespace() == null ? configuration.getNamespace() : declaration.namespace();
        String name = declaration.name();

        List<V1Secret> secrets;
        if (StringUtils.isNotEmpty(name)) {
            V1Secret secret = readIfExists(() -> client.readNamespacedSecret(name, namespace, null).block());
            secrets = secret == null ? Collections.emptyList() : List.of(secret);
        } else {
            String labelSelector = KubernetesConfigUtils.computePodLabelSelector(client, declaration.podLabels(), namespace, declaration.labels(), declaration.exceptionOnPodLabelsMissing()).block();
            V1SecretList secretList = client.listNamespacedSecret(namespace, null, null, null, null, labelSelector, null, null, null, null, null, null).block();
            secrets = secretList == null ? Collections.emptyList() : secretList.getItems();
        }
        return toPropertySource(secrets, V1Secret.class, KubernetesConfigUtils::secretAsMap);
    }
}
