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
package io.micronaut.kubernetes.client.openapi.watcher.mapper;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves item type name (for example {@code io.micronaut.kubernetes.client.openapi.model.V1Secret})
 * for given list type name (for example {@code io.micronaut.kubernetes.client.openapi.model.V1SecretList}).
 *
 * <p>
 * The resolver finds all implementations of {@link TypeNameMapper} interface and loads type mappings from it.
 * If given list type name is not found in loaded type mappings, the item type name will be created from
 * the list type name by removing {@code List} suffix.
 * </p>
 *
 * @since 8.0
 */
@Singleton
@BootstrapContextCompatible
@Requires(beans = KubernetesClientConfiguration.class)
public final class TypeNameResolver {

    private final Map<String, String> mappings = new HashMap<>();

    TypeNameResolver(List<TypeNameMapper> mappers) {
        for (TypeNameMapper mapper : mappers) {
            mappings.putAll(mapper.getMappings());
        }
    }

    /**
     * Resolves item type name for given list type name. If given list type name is not found in loaded type mappings,
     * the item type name will be created from the list type name by removing {@code List} suffix.
     *
     * @param listTypeName the kubernetes list type name
     * @return the kubernetes item type name
     */
    public String resolveItemTypeName(String listTypeName) {
        return mappings.containsKey(listTypeName) ? mappings.get(listTypeName) : listTypeName.substring(0, listTypeName.indexOf("List"));
    }
}
