package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.configuration.KubernetesPropertySourceImporter.ImportDeclaration;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMapList;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
@Requires(beans = KubernetesConfiguration.class)
final class KubernetesConfigMapImportSupport extends KubernetesObjectImportSupport {

    private final CoreV1ApiReactor client;
    private final KubernetesConfiguration configuration;

    KubernetesConfigMapImportSupport(CoreV1ApiReactor client,
                                     KubernetesConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    @NonNull
    @Override
    Optional<PropertySource> importPropertySource(@NonNull ImportDeclaration declaration,
                                                  @NonNull Collection<PropertySourceLoader> propertySourceLoaders) {
        KubernetesLegacyImportMode.registerConfigMapImport();

        String namespace = declaration.namespace() == null ? configuration.getNamespace() : declaration.namespace();
        String name = declaration.name();

        List<V1ConfigMap> configMaps;
        if (StringUtils.isNotEmpty(name)) {
            V1ConfigMap configMap = readIfExists(() -> client.readNamespacedConfigMap(name, namespace, null).block());
            configMaps = configMap == null ? Collections.emptyList() : List.of(configMap);
        } else {
            String labelSelector = KubernetesConfigUtils.computePodLabelSelector(client, declaration.podLabels(), namespace, declaration.labels(), declaration.exceptionOnPodLabelsMissing()).block();
            V1ConfigMapList configMapList = client.listNamespacedConfigMap(namespace, null, null, null, null, labelSelector, null, null, null, null, null, null).block();
            configMaps = configMapList == null ? Collections.emptyList() : configMapList.getItems();
        }
        return toPropertySource(configMaps, V1ConfigMap.class, item -> KubernetesConfigUtils.configMapAsMap(item, propertySourceLoaders));
    }
}
