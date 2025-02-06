/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.EmptyPropertySource;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.EnvironmentPropertySource;
import io.micronaut.context.env.PropertySource;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMapList;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import io.micronaut.kubernetes.client.openapi.model.V1SecretList;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import io.micronaut.kubernetes.client.openapi.util.KubernetesUtils;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * A {@link ConfigurationClient} implementation that provides {@link PropertySource}s
 * read from Kubernetes ConfigMaps and Secrets.
 *
 * @author Álvaro Sánchez-Mariscal
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(property = ConfigurationClient.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@BootstrapContextCompatible
final class KubernetesConfigurationClient implements ConfigurationClient {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigurationClient.class);

    private static final Map<String, PropertySource> propertySources = new ConcurrentHashMap<>();

    private final CoreV1ApiReactor client;
    private final KubernetesConfiguration configuration;

    /**
     * @param client        A Core HTTP Client to query the Kubernetes API.
     * @param configuration The configuration properties
     */
    KubernetesConfigurationClient(CoreV1ApiReactor client, KubernetesConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    @Override
    public Publisher<PropertySource> getPropertySources(Environment environment) {
        if (!propertySources.isEmpty()) {
            LOG.trace("Returning cached PropertySources");
            return Flux.fromIterable(propertySources.values());
        } else {
            LOG.trace("PropertySource cache is empty");
            return Flux.from(getPropertySourcesFromConfigMaps()).mergeWith(getPropertySourcesFromSecrets());
        }
    }

    @Override
    public String getDescription() {
        return "kubernetes";
    }

    /**
     * Adds the given {@link PropertySource} to the cache.
     *
     * @param propertySource The property source to add
     */
    static void addPropertySourceToCache(PropertySource propertySource) {
        String propertySourceName = propertySource.getName();
        LOG.trace("Adding property source {} to cache", propertySourceName);
        propertySources.put(propertySourceName, propertySource);
    }

    /**
     * Removes the given {@link PropertySource} name from the cache.
     *
     * @param name The property source name
     */
    static void removePropertySourceFromCache(String name) {
        LOG.trace("Removing property source {} from cache", name);
        propertySources.remove(name);
    }

    /**
     * @return the property source cache.
     */
    static Map<String, PropertySource> getPropertySourceCache() {
        return propertySources;
    }

    private Flux<PropertySource> getPropertySourcesFromConfigMaps() {
        Flux<PropertySource> propertySourceFlux = Flux.empty();
        KubernetesConfiguration.KubernetesConfigMapsConfiguration configMapsConfiguration = configuration.getConfigMaps();
        if (configMapsConfiguration.isEnabled()) {
            Collection<String> mountedVolumePaths = configMapsConfiguration.getPaths();
            if (mountedVolumePaths.isEmpty() || configMapsConfiguration.isUseApi()) {
                propertySourceFlux = propertySourceFlux.mergeWith(readConfigMapsFromApi());
            }
            if (!mountedVolumePaths.isEmpty()) {
                propertySourceFlux = propertySourceFlux.mergeWith(readConfigMapsFromMountedVolumes(mountedVolumePaths));
            }
        } else {
            LOG.debug("Kubernetes config maps access is disabled");
        }
        return propertySourceFlux;
    }

    private Flux<PropertySource> readConfigMapsFromApi() {
        String namespace = configuration.getNamespace();
        LOG.debug("Reading ConfigMaps from the Kubernetes API, namespace={}", namespace);

        KubernetesConfiguration.KubernetesConfigMapsConfiguration configMapsConfiguration = configuration.getConfigMaps();
        Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(configMapsConfiguration.getIncludes());
        Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(configMapsConfiguration.getExcludes());
        List<String> podLabels = configMapsConfiguration.getPodLabels();
        Map<String, String> labels = configMapsConfiguration.getLabels();
        boolean exceptionOnPodLabelsMissing = configMapsConfiguration.isExceptionOnPodLabelsMissing();

        return KubernetesConfigUtils.computePodLabelSelector(client, podLabels, namespace, labels, exceptionOnPodLabelsMissing)
            .doOnError(throwable -> LOG.error("Failed to compute pod label selector", throwable))
            .doOnNext(labelSelector -> LOG.trace("Going to list ConfigMaps from namespace [{}] with label selector [{}]", namespace, labelSelector))
            .flatMap(labelSelector -> client.listNamespacedConfigMap(namespace, null, null, null, null, labelSelector, null, null, null, null, null, null))
            .doOnError(throwable -> LOG.error("Failed to list ConfigMaps in the namespace [{}]", namespace, throwable))
            .onErrorResume(throwable -> exceptionOnPodLabelsMissing ? Mono.error(throwable) : Mono.just(new V1ConfigMapList(new ArrayList<>())))
            .doOnNext(configMapList -> LOG.debug("Found {} config maps. Applying includes/excludes filters (if any)", configMapList.getItems().size()))
            .flux()
            .flatMap(configMapList -> Flux.merge(
                Flux.just(KubernetesConfigUtils.kubernetesListAsPropertySource(configMapList)),
                Flux.fromIterable(configMapList.getItems())
                    .filter(includesFilter.and(excludesFilter))
                    .map(KubernetesConfigUtils::configMapAsPropertySource)
            ))
            .filter(propertySource -> !(propertySource instanceof EmptyPropertySource))
            .doOnNext(KubernetesConfigurationClient::addPropertySourceToCache);
    }

    private Flux<PropertySource> readConfigMapsFromMountedVolumes(Collection<String> paths) {
        LOG.debug("Reading ConfigMaps from mounted volumes: {}", paths);
        List<PropertySource> propertySources = new ArrayList<>();
        paths.forEach(path -> {
            LOG.trace("Reading ConfigMaps from mounted volume: {}", path);
            Map<String, String> configMapFiles = readFiles(Paths.get(path));
            LOG.debug("ConfigMaps file found on path '{}': {}", path, configMapFiles.keySet());
            if (!configMapFiles.isEmpty()) {
                List<PropertySource> mountedMapPropertySources = KubernetesConfigUtils.configMapAsPropertySource(path, configMapFiles);
                mountedMapPropertySources.forEach(KubernetesConfigurationClient::addPropertySourceToCache);
                propertySources.addAll(mountedMapPropertySources);
            }
        });
        return Flux.fromIterable(propertySources);
    }

    private Publisher<PropertySource> getPropertySourcesFromSecrets() {
        Flux<PropertySource> propertySourceFlux = Flux.empty();
        KubernetesConfiguration.KubernetesSecretsConfiguration secretsConfiguration = configuration.getSecrets();
        if (secretsConfiguration.isEnabled()) {
            Collection<String> mountedVolumePaths = secretsConfiguration.getPaths();
            if (mountedVolumePaths.isEmpty() || secretsConfiguration.isUseApi()) {
                propertySourceFlux = propertySourceFlux.mergeWith(readSecretsFromApi());
            }
            if (!mountedVolumePaths.isEmpty()) {
                propertySourceFlux = propertySourceFlux.mergeWith(readSecretsFromMountedVolumes(mountedVolumePaths));
            }
        } else {
            LOG.debug("Kubernetes secrets access is disabled");
        }
        return propertySourceFlux;
    }

    private Flux<PropertySource> readSecretsFromApi() {
        String namespace = configuration.getNamespace();
        LOG.debug("Reading Secrets from the Kubernetes API, namespace={}", namespace);

        KubernetesConfiguration.KubernetesSecretsConfiguration secretsConfiguration = configuration.getSecrets();
        Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(secretsConfiguration.getIncludes());
        Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(secretsConfiguration.getExcludes());
        Predicate<V1Secret> secretTypeFilter = KubernetesUtils.getIncludeOpaqueSecretTypeFilter();
        List<String> podLabels = secretsConfiguration.getPodLabels();
        Map<String, String> labels = secretsConfiguration.getLabels();
        boolean exceptionOnPodLabelsMissing = secretsConfiguration.isExceptionOnPodLabelsMissing();

        return KubernetesConfigUtils.computePodLabelSelector(client, podLabels, namespace, labels, exceptionOnPodLabelsMissing)
            .doOnError(throwable -> LOG.error("Failed to compute pod label selector", throwable))
            .doOnNext(labelSelector -> LOG.trace("Going to list Secrets from namespace [{}] with label selector [{}]", namespace, labelSelector))
            .flatMap(labelSelector -> client.listNamespacedSecret(namespace, null, null, null, null, labelSelector, null, null, null, null, null, null))
            .doOnError(throwable -> LOG.error("Failed to list Secrets in the namespace [{}]", namespace, throwable))
            .onErrorResume(throwable -> exceptionOnPodLabelsMissing ? Mono.error(throwable) : Mono.just(new V1SecretList(new ArrayList<>())))
            .doOnNext(secretList -> LOG.debug("Found {} secrets. Filtering Opaque secrets and includes/excludes (if any)", secretList.getItems().size()))
            .flux()
            .flatMap(secretList -> Flux.merge(
                Flux.just(KubernetesConfigUtils.kubernetesListAsPropertySource(secretList)),
                Flux.fromIterable(secretList.getItems())
                    .filter(secretTypeFilter.and(includesFilter).and(excludesFilter))
                    .map(KubernetesConfigUtils::secretAsPropertySource)
            ))
            .filter(propertySource -> !(propertySource instanceof EmptyPropertySource))
            .doOnNext(KubernetesConfigurationClient::addPropertySourceToCache);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Flux<PropertySource> readSecretsFromMountedVolumes(Collection<String> paths) {
        LOG.debug("Reading Secrets from mounted volumes: {}", paths);
        List<PropertySource> propertySources = new ArrayList<>();
        paths.forEach(path -> {
            LOG.trace("Reading Secrets from mounted volume: {}", path);
            Map<String, Object> secretFiles = (Map) readFiles(Paths.get(path));
            LOG.debug("Secrets file found on path '{}': {}", path, secretFiles.keySet());
            if (!secretFiles.isEmpty()) {
                String propertySourceName = KubernetesConfigUtils.createPropertySourceName(path, V1Secret.class);
                PropertySource propertySource = PropertySource.of(propertySourceName, secretFiles, EnvironmentPropertySource.POSITION + 150);
                addPropertySourceToCache(propertySource);
                propertySources.add(propertySource);
            }
        });
        return Flux.fromIterable(propertySources);
    }

    private Map<String, String> readFiles(Path dirPath) {
        Map<String, String> fileContents = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path filePath : stream) {
                if (!Files.isRegularFile(filePath)) {
                    LOG.trace("Skipping not regular file: {}", filePath);
                    continue;
                }
                try {
                    String fileContent = new String(Files.readAllBytes(filePath));
                    fileContents.put(filePath.getFileName().toString(), fileContent);
                    LOG.trace("Found file: {}", filePath);
                } catch (IOException e) {
                    LOG.error("Failed to read file content from path: {}", filePath, e);
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to read files from directory path: {}", dirPath, e);
        }
        return fileContents;
    }
}
