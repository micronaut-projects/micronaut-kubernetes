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

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter.ImportContext;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpStatus;
import io.micronaut.kubernetes.util.KubernetesUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base support for importing Micronaut property sources from Kubernetes resources.
 */
abstract sealed class KubernetesObjectImportSupport permits KubernetesConfigMapImportSupport, KubernetesSecretImportSupport {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesObjectImportSupport.class);

    /**
     * Imports a property source for the supplied declaration and applies declaration-specific error handling.
     *
     * @param context The import context containing the declaration and environment
     * @return The imported property source when one can be resolved
     */
    @NonNull
    Optional<PropertySource> importPropertySource(@NonNull ImportContext<ImportDeclaration> context) {
        ImportDeclaration declaration = context.importDeclaration();
        LOG.debug("Started property source import for declaration: {}", declaration);
        try {
            PropertySource cachedPropertySource = PropertySourceCache.get(declaration);
            if (cachedPropertySource != null) {
                if (declaration.watch()) {
                    LOG.debug("Found cached property source created from watched declaration={}. Checking whether it requires refresh", declaration);
                    AtomicInteger refreshCount = ImportDeclarationWatchIndex.getRefreshCount(declaration);
                    if (refreshCount.getAndSet(0) == 0) {
                        return Optional.of(cachedPropertySource);
                    }
                    LOG.debug("Refresh required for cached property source created from watched declaration={}", declaration);
                    PropertySourceCache.remove(declaration);
                } else {
                    LOG.debug("Found cached property source created from unwatched declaration={}", declaration);
                    return Optional.of(cachedPropertySource);
                }
            }

            Collection<PropertySourceLoader> propertySourceLoaders = context.environment().getPropertySourceLoaders();

            Optional<PropertySource> propertySource = StringUtils.isNotEmpty(declaration.name())
                ? importPropertySourceByNameSelector(declaration, propertySourceLoaders)
                : importPropertySourceByLabelsSelector(declaration, propertySourceLoaders);

            propertySource.ifPresent(source -> PropertySourceCache.add(declaration, source));

            LOG.debug("Completed property source import for declaration: {}", declaration);
            return propertySource;
        } catch (RuntimeException e) {
            if (declaration.terminateStartupOnException()
                || (e instanceof ConfigurationException && declaration.exceptionOnPodLabelsMissing())) {
                throw e;
            }
            LOG.error("Failed to import property source for declaration {}, but didn't terminate since terminateStartupOnException=false",
                declaration, e);
            return Optional.empty();
        }
    }

    @NonNull
    abstract Optional<PropertySource> importPropertySourceByNameSelector(
        @NonNull ImportDeclaration declaration,
        @NonNull Collection<PropertySourceLoader> propertySourceLoaders);

    @NonNull
    abstract Optional<PropertySource> importPropertySourceByLabelsSelector(
        @NonNull ImportDeclaration declaration,
        @NonNull Collection<PropertySourceLoader> propertySourceLoaders);

    /**
     * Converts Kubernetes objects into a single Micronaut property source.
     *
     * @param objects       The Kubernetes objects to transform
     * @param objectType    The Kubernetes object type used when creating the property source name
     * @param dataExtractor Extracts property values from each Kubernetes object
     * @param <T>           The Kubernetes object type
     * @return The resulting property source when at least one object contributes data
     */
    @NonNull
    <T extends KubernetesObject> Optional<PropertySource> toPropertySource(
        @Nullable List<T> objects,
        @NonNull Class<T> objectType,
        @NonNull Function<? super T, Map<String, Object>> dataExtractor) {

        if (CollectionUtils.isEmpty(objects)) {
            return Optional.empty();
        }

        List<String> resourceNames = new ArrayList<>();
        Map<String, Object> propertySourceData = new HashMap<>();

        objects.forEach(object -> {
            resourceNames.add(object.getMetadata().getName());
            String resVersionPropertyName = KubernetesUtils.createResVersionPropertyName(object);
            String resVersionPropertyValue = object.getMetadata().getResourceVersion();
            propertySourceData.put(resVersionPropertyName, resVersionPropertyValue);
            Map<String, Object> data = dataExtractor.apply(object);
            if (CollectionUtils.isNotEmpty(data)) {
                propertySourceData.putAll(data);
            }
        });

        if (CollectionUtils.isEmpty(propertySourceData)) {
            return Optional.empty();
        }

        String propertySourceName = KubernetesUtils.createPropertySourceName(String.join(";", resourceNames), objectType);
        return Optional.of(PropertySource.of(propertySourceName, propertySourceData, KubernetesUtils.API_PROPERTY_SOURCE_PRIORITY));
    }

    /**
     * Executes a Kubernetes read operation and treats HTTP 404 responses as an absent resource.
     *
     * @param readFn The read operation to execute
     * @param <T>    The read result type
     * @return The read value, or {@code null} when the resource does not exist
     */
    @Nullable
    <T> T readIfExists(@NonNull Supplier<T> readFn) {
        try {
            return readFn.get();
        } catch (Exception e) {
            Throwable root = Exceptions.unwrap(e);
            if (root instanceof ApiException apiException && apiException.getCode() == HttpStatus.NOT_FOUND.getCode()) {
                return null;
            }
            throw e;
        }
    }
}
