package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter.ImportContext;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.configuration.KubernetesPropertySourceImporter.ImportDeclaration;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

sealed abstract class KubernetesObjectImportSupport permits KubernetesConfigMapImportSupport, KubernetesSecretImportSupport {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesObjectImportSupport.class);

    @NonNull
    Optional<PropertySource> importPropertySource(@NonNull ImportContext<ImportDeclaration> context) {
        ImportDeclaration declaration = context.importDeclaration();
        LOG.debug("Started property source import for declaration: {}", declaration);
        try {
            Optional<PropertySource> propertySource = importPropertySource(declaration, context.environment().getPropertySourceLoaders());
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
    abstract Optional<PropertySource> importPropertySource(
        @NonNull ImportDeclaration declaration,
        @NonNull Collection<PropertySourceLoader> propertySourceLoaders);

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
            Map<String, Object> data = dataExtractor.apply(object);
            if (CollectionUtils.isNotEmpty(data)) {
                resourceNames.add(object.getMetadata().getName());
                propertySourceData.putAll(data);
                String resVersionPropertyName = KubernetesConfigUtils.createResVersionPropertyName(object);
                String resVersionPropertyValue = object.getMetadata().getResourceVersion();
                propertySourceData.put(resVersionPropertyName, resVersionPropertyValue);
            }
        });

        if (CollectionUtils.isEmpty(propertySourceData)) {
            return Optional.empty();
        }

        String propertySourceName = KubernetesConfigUtils.createPropertySourceName(String.join(";", resourceNames), objectType);
        return Optional.of(PropertySource.of(propertySourceName, propertySourceData, KubernetesConfigUtils.API_PROPERTY_SOURCE_PRIORITY));
    }

    @Nullable
    <T> T readIfExists(@NonNull Supplier<T> readFn) {
        try {
            return readFn.get();
        } catch (HttpClientResponseException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }
}
