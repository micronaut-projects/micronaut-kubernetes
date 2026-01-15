package io.micronaut.kubernetes.client.openapi.informer;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.watcher.mapper.TypeNameMapper;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@BootstrapContextCompatible
@Requires(beans = KubernetesClientConfiguration.class)
final class TypeNameResolver {

    private final Map<String, String> mappings = new HashMap<>();

    TypeNameResolver(List<TypeNameMapper> mappers) {
        for (TypeNameMapper mapper : mappers) {
            mappings.putAll(mapper.getMappings());
        }
    }

    String getItemTypeName(String listTypeName) {
        return mappings.containsKey(listTypeName)  ? mappings.get(listTypeName) : listTypeName.substring(0, listTypeName.indexOf("List"));
    }
}
