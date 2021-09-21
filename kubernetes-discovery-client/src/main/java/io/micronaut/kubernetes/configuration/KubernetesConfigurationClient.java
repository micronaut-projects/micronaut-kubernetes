/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.kubernetes.configuration;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1ConfigMapListBuilder;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.openapi.models.V1SecretListBuilder;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.EnvironmentPropertySource;
import io.micronaut.context.env.PropertySource;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.kubernetes.util.KubernetesUtils;
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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static io.micronaut.kubernetes.util.KubernetesUtils.computePodLabelSelector;
import static java.util.Collections.singletonMap;

/**
 * A {@link ConfigurationClient} implementation that provides {@link PropertySource}s read from Kubernetes ConfigMap's.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(property = ConfigurationClient.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@BootstrapContextCompatible
public class KubernetesConfigurationClient implements ConfigurationClient {

    public static final String CONFIG_MAP_LIST_RESOURCE_VERSION = "configMapListResourceVersion";
    public static final String CONFIG_MAP_RESOURCE_VERSION = "configMapResourceVersion";
    public static final String KUBERNETES_CONFIG_MAP_LIST_NAME = "Kubernetes ConfigMapList";
    public static final String KUBERNETES_CONFIG_MAP_NAME_SUFFIX = " (Kubernetes ConfigMap)";
    public static final String KUBERNETES_SECRET_NAME_SUFFIX = " (Kubernetes Secret)";
    public static final String OPAQUE_SECRET_TYPE = "Opaque";

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigurationClient.class);

    private static Map<String, PropertySource> propertySources = new ConcurrentHashMap<>();

    private final CoreV1ApiReactorClient client;
    private final KubernetesConfiguration configuration;

    /**
     * @param client        An Core HTTP Client to query the Kubernetes API.
     * @param configuration The configuration properties
     */
    public KubernetesConfigurationClient(CoreV1ApiReactorClient client, KubernetesConfiguration configuration) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing {}", getClass().getName());
        }
        this.client = client;
        this.configuration = configuration;
    }

    /**
     * Retrieves all of the {@link PropertySource} registrations for the given environment.
     *
     * @param environment The environment
     * @return A {@link Publisher} that emits zero or many {@link PropertySource} instances discovered for the given environment
     */
    @Override
    public Publisher<PropertySource> getPropertySources(Environment environment) {
        if (!propertySources.isEmpty()) {
            LOG.trace("Found cached PropertySources. Returning them");
            return Flux.fromIterable(propertySources.values());
        } else {
            LOG.trace("PropertySource cache is empty");
            return Flux.from(getPropertySourcesFromConfigMaps()).mergeWith(getPropertySourcesFromSecrets());
        }
    }

    /**
     * A description that describes this object.
     *
     * @return The description
     */
    @Override
    public @NonNull
    String getDescription() {
        return "kubernetes";
    }

    /**
     * Adds the given {@link PropertySource} to the cache.
     *
     * @param propertySource The property source to add
     */
    public static void addPropertySourceToCache(PropertySource propertySource) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding property source {} to cache", propertySource.getName());
        }
        propertySources.put(propertySource.getName(), propertySource);
    }

    /**
     * Removes the given {@link PropertySource} name from the cache.
     *
     * @param name The property source name
     */
    static void removePropertySourceFromCache(String name) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Removing property source {} from cache", name);
        }
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

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading ConfigMaps from the Kubernetes API");
                }

                Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(configMapsConfiguration.getIncludes());
                Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(configMapsConfiguration.getExcludes());
                Map<String, String> labels = configMapsConfiguration.getLabels();
                boolean exceptionOnPodLabelsMissing = configuration.getConfigMaps().isExceptionOnPodLabelsMissing();

                Flux<PropertySource> configMapListFlux = computePodLabelSelector(client,
                        configuration.getConfigMaps().getPodLabels(), configuration.getNamespace(), labels, exceptionOnPodLabelsMissing)
                        .doOnError(throwable -> LOG.error("Failed to compute pod label selector: " + throwable.getMessage(), throwable))
                        .doOnNext(labelSelector -> {
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Going to list ConfigMaps from namespace [{}] with label selector [{}]", configuration.getNamespace(), labelSelector);
                            }
                        })
                        .flatMap(labelSelector ->
                                client.listNamespacedConfigMap(configuration.getNamespace(), null, null, null, null, labelSelector, null, null, null, null))
                        .doOnError(ApiException.class, throwable -> LOG.error("Error to list ConfigMaps in the namespace [" + configuration.getNamespace() + "]: " + throwable.getResponseBody(), throwable))
                        .onErrorResume(throwable -> exceptionOnPodLabelsMissing
                                ? Mono.error(throwable)
                                : Mono.just(new V1ConfigMapListBuilder().withItems(new ArrayList<>()).build()))
                        .doOnNext(configMapList -> {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Found {} config maps. Applying includes/excludes filters (if any)", configMapList.getItems().size());
                            }
                        })
                        .flux()
                        .flatMap(configMapList -> Flux.merge(
                                Flux.just(configMapListAsPropertySource(configMapList)),
                                Flux.fromIterable(configMapList.getItems())
                                        .filter(includesFilter)
                                        .filter(excludesFilter)
                                        .doOnNext(configMap -> {
                                            if (LOG.isDebugEnabled()) {
                                                LOG.debug("Adding config map with name {}", configMap.getMetadata().getName());
                                            }
                                        })
                                        .map(KubernetesUtils::configMapAsPropertySource)
                        ));
                propertySourceFlux = propertySourceFlux.mergeWith(configMapListFlux);
            }

            if (!mountedVolumePaths.isEmpty()) {

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading ConfigMaps from the following mounted volumes: {}", mountedVolumePaths);
                }

                List<PropertySource> propertySources = new ArrayList<>();
                mountedVolumePaths.stream()
                        .map(Paths::get)
                        .forEach(path -> {
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Processing path: {}", path);
                            }

                            final HashMap<String, String> configMapFiles = new HashMap<>();

                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                                for (Path file : stream) {
                                    if (!Files.isRegularFile(file)) {
                                        if (LOG.isInfoEnabled()) {
                                            LOG.info("{} is not regular file, skipping ", file.getFileName());
                                        }
                                        continue;
                                    }

                                    String key = file.getFileName().toString();
                                    String value = new String(Files.readAllBytes(file));

                                    if (LOG.isTraceEnabled()) {
                                        LOG.trace("Processing file: {}:", key);
                                    }

                                    configMapFiles.put(key, value);
                                }
                            } catch (IOException e) {
                                if (LOG.isWarnEnabled()) {
                                    LOG.warn("Exception occurred when reading configmap from path: {}", path);
                                    LOG.warn(e.getMessage(), e);
                                }
                            }

                            List<PropertySource> mountedMapPropertySources = KubernetesUtils.configMapAsPropertySource(path.toString(), configMapFiles);
                            mountedMapPropertySources.forEach(KubernetesConfigurationClient::addPropertySourceToCache);
                            propertySources.addAll(mountedMapPropertySources);
                        });

                propertySourceFlux = propertySourceFlux.mergeWith(Flux.fromIterable(propertySources));
            }
        }
        return propertySourceFlux;
    }

    /**
     * Converts a {@link V1ConfigMapList} into a {@link PropertySource}.
     *
     * @param configMapList the ConfigMapList
     * @return A PropertySource
     */
    private static PropertySource configMapListAsPropertySource(V1ConfigMapList configMapList) {
        String resourceVersion = configMapList.getMetadata() != null ? configMapList.getMetadata().getResourceVersion() : "-1";
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding config map list with version {}", resourceVersion);
        }
        return PropertySource.of(KUBERNETES_CONFIG_MAP_LIST_NAME, singletonMap(CONFIG_MAP_LIST_RESOURCE_VERSION, resourceVersion), EnvironmentPropertySource.POSITION + 100);
    }

    private Publisher<PropertySource> getPropertySourcesFromSecrets() {
        Flux<PropertySource> propertySourceFlowable = Flux.empty();
        if (configuration.getSecrets().isEnabled()) {
            Collection<String> mountedVolumePaths = configuration.getSecrets().getPaths();

            if (mountedVolumePaths.isEmpty() || configuration.getSecrets().isUseApi()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading Secrets from the Kubernetes API");
                }

                Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(configuration.getSecrets().getIncludes());
                Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(configuration.getSecrets().getExcludes());
                Map<String, String> labels = configuration.getSecrets().getLabels();
                boolean exceptionOnPodLabelsMissing = configuration.getSecrets().isExceptionOnPodLabelsMissing();

                Flux<PropertySource> secretListFlowable = computePodLabelSelector(client,
                    configuration.getSecrets().getPodLabels(), configuration.getNamespace(), labels, exceptionOnPodLabelsMissing)
                        .flatMap(labelSelector -> client.listNamespacedSecret(configuration.getNamespace(), null, null, null, null, labelSelector, null, null, null, null))
                        .doOnError(ApiException.class, throwable -> LOG.error("Failed to list Secrets in the namespace [" + configuration.getNamespace() + "]: " + throwable.getResponseBody(), throwable))
                        .onErrorResume(throwable -> exceptionOnPodLabelsMissing
                                                       ? Mono.error(throwable)
                                                       : Mono.just(new V1SecretListBuilder().withItems(new ArrayList<>()).build()))
                        .doOnNext(secretList -> {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Found {} secrets. Filtering Opaque secrets and includes/excludes (if any)", secretList.getItems().size());
                            }
                        })
                        .flatMapIterable(V1SecretList::getItems)
                        .filter(secret -> Objects.equals(secret.getType(), OPAQUE_SECRET_TYPE))
                        .filter(includesFilter)
                        .filter(excludesFilter)
                        .doOnNext(secret -> {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Adding secret with name {}", secret.getMetadata().getName());
                            }
                        })
                        .map(KubernetesUtils::secretAsPropertySource);

                propertySourceFlowable = propertySourceFlowable.mergeWith(secretListFlowable);
            }

            if (!mountedVolumePaths.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading Secrets from the following mounted volumes: {}", mountedVolumePaths);
                }

                List<PropertySource> propertySources = new ArrayList<>();
                mountedVolumePaths.stream()
                        .map(Paths::get)
                        .forEach(path -> {
                            LOG.trace("Processing path: {}", path);
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                                Map<String, Object> propertySourceContents = new HashMap<>();
                                for (Path file : stream) {
                                    if (!Files.isDirectory(file)) {
                                        String key = file.getFileName().toString();
                                        String value = new String(Files.readAllBytes(file));
                                        if (LOG.isTraceEnabled()) {
                                            LOG.trace("Processing key: {}", key);
                                        }
                                        propertySourceContents.put(key, value);
                                    }
                                }
                                String propertySourceName = path.toString() + KUBERNETES_SECRET_NAME_SUFFIX;
                                int priority = EnvironmentPropertySource.POSITION + 150;
                                PropertySource propertySource = PropertySource.of(propertySourceName, propertySourceContents, priority);
                                addPropertySourceToCache(propertySource);
                                propertySources.add(propertySource);
                            } catch (IOException e) {
                                LOG.warn("Exception occurred when reading secrets from path: {}", path);
                                LOG.warn(e.getMessage(), e);
                            }
                        });

                propertySourceFlowable = propertySourceFlowable.mergeWith(Flux.fromIterable(propertySources));
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Kubernetes secrets access is disabled");
            }
        }
        return propertySourceFlowable;
    }

}
