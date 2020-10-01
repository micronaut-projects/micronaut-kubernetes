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

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.EnvironmentPropertySource;
import io.micronaut.context.env.PropertySource;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.KubernetesObject;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapList;
import io.micronaut.kubernetes.client.v1.secrets.SecretList;
import io.micronaut.kubernetes.util.KubernetesUtils;
import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.micronaut.kubernetes.client.v1.secrets.Secret.OPAQUE_SECRET_TYPE;
import static io.micronaut.kubernetes.util.KubernetesUtils.computePodLabels;

/**
 * A {@link ConfigurationClient} implementation that provides {@link PropertySource}s read from Kubernetes ConfigMap's.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(beans = KubernetesClient.class)
@Requires(property = ConfigurationClient.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@BootstrapContextCompatible
public class KubernetesConfigurationClient implements ConfigurationClient {

    public static final String CONFIG_MAP_RESOURCE_VERSION = "configMapResourceVersion";
    public static final String KUBERNETES_CONFIG_MAP_NAME_SUFFIX = " (Kubernetes ConfigMap)";
    public static final String KUBERNETES_SECRET_NAME_SUFFIX = " (Kubernetes Secret)";

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfigurationClient.class);

    private static Map<String, PropertySource> propertySources = new ConcurrentHashMap<>();

    private final KubernetesClient client;
    private final KubernetesConfiguration configuration;

    /**
     * @param client        An HTTP Client to query the Kubernetes API.
     * @param configuration The configuration properties
     */
    public KubernetesConfigurationClient(KubernetesClient client, KubernetesConfiguration configuration) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing {}", getClass().getName());
        }
        this.client = client;
        this.configuration = configuration;
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
            LOG.trace("Removing property source {} to cache", name);
        }
        propertySources.remove(name);
    }

    /**
     * @return the property source cache.
     */
    static Map<String, PropertySource> getPropertySourceCache() {
        return propertySources;
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
            return Flowable.fromIterable(propertySources.values());
        } else {
            LOG.trace("PropertySource cache is empty");
            return getPropertySourcesFromConfigMaps().mergeWith(getPropertySourcesFromSecrets());
        }
    }

    /**
     * A description that describes this object.
     *
     * @return The description
     */
    @Override
    public String getDescription() {
        return KubernetesClient.SERVICE_ID;
    }

    private Flowable<PropertySource> getPropertySourcesFromConfigMaps() {
        Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(configuration.getConfigMaps().getIncludes());
        Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(configuration.getConfigMaps().getExcludes());
        Map<String, String> labels = configuration.getConfigMaps().getLabels();

        return computePodLabels(client, configuration.getConfigMaps().getPodLabels(), configuration.getNamespace()).flatMap(podLabels -> {
            podLabels.putAll(labels);
            String labelSelector = KubernetesUtils.computeLabelSelector(podLabels);
            return client.listConfigMaps(configuration.getNamespace(), labelSelector);
        })
                .doOnError(throwable -> LOG.error("Error while trying to list all Kubernetes ConfigMaps in the namespace [" + configuration.getNamespace() + "]", throwable))
                .onErrorReturn(throwable -> new ConfigMapList())
                .doOnNext(configMapList -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found {} config maps. Applying includes/excludes filters (if any)", configMapList.getItems().size());
                    }
                })
                .flatMapIterable(ConfigMapList::getItems)
                .filter(includesFilter)
                .filter(excludesFilter)
                .doOnNext(configMap -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Adding config map with name {}", configMap.getMetadata().getName());
                    }
                })
                .map(KubernetesUtils::configMapAsPropertySource);
    }

    private Flowable<PropertySource> getPropertySourcesFromSecrets() {
        Flowable<PropertySource> propertySourceFlowable = Flowable.empty();
        if (configuration.getSecrets().isEnabled()) {
            Collection<String> mountedVolumePaths = configuration.getSecrets().getPaths();
            if (mountedVolumePaths.isEmpty() || configuration.getSecrets().isUseApi()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading Secrets from the Kubernetes API");
                }

                Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(configuration.getSecrets().getIncludes());
                Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(configuration.getSecrets().getExcludes());
                Map<String, String> labels = configuration.getSecrets().getLabels();
                Flowable<PropertySource> secretListFlowable = computePodLabels(client, configuration.getSecrets().getPodLabels(), configuration.getNamespace()).flatMap(podLabels -> {
                    podLabels.putAll(labels);
                    String labelSelector = KubernetesUtils.computeLabelSelector(podLabels);
                    return Flowable.fromPublisher(client.listSecrets(configuration.getNamespace(), labelSelector));
                })
                        .doOnError(throwable -> LOG.error("Error while trying to list all Kubernetes Secrets in the namespace [" + configuration.getNamespace() + "]", throwable))
                        .onErrorReturn(throwable -> new SecretList())
                        .doOnNext(secretList -> {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Found {} secrets. Filtering Opaque secrets and includes/excludes (if any)", secretList.getItems().size());
                            }
                        })
                        .flatMapIterable(SecretList::getItems)
                        .filter(secret -> secret.getType().equals(OPAQUE_SECRET_TYPE))
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

                propertySourceFlowable = propertySourceFlowable.mergeWith(Flowable.fromIterable(propertySources));
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Kubernetes secrets access is disabled");
            }
        }
        return propertySourceFlowable;
    }

}
