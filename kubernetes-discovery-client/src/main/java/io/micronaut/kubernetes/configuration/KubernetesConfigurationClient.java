/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceReader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.jackson.env.JsonPropertySourceLoader;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapList;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.micronaut.kubernetes.client.v1.secrets.SecretList;
import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.micronaut.kubernetes.client.v1.secrets.Secret.OPAQUE_SECRET_TYPE;

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

    private static final List<PropertySourceReader> PROPERTY_SOURCE_READERS = Arrays.asList(
            new YamlPropertySourceLoader(),
            new JsonPropertySourceLoader(),
            new PropertiesPropertySourceLoader());

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

    /**
     * Converts a {@link ConfigMap} into a {@link PropertySource}.
     *
     * @param configMap the ConfigMap
     * @return A PropertySource
     */
    static PropertySource configMapAsPropertySource(ConfigMap configMap) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing PropertySources for ConfigMap: {}", configMap);
        }
        String name = getPropertySourceName(configMap);
        Map<String, String> data = configMap.getData();
        Map.Entry<String, String> entry = data.entrySet().iterator().next();
        if (data.size() > 1 || !getExtension(entry.getKey()).isPresent()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Considering this ConfigMap as containing multiple literal key/values");
            }
            data.putIfAbsent(CONFIG_MAP_RESOURCE_VERSION, configMap.getMetadata().getResourceVersion());
            Map<String, Object> propertySourceData = new HashMap<>(data);
            return PropertySource.of(name, propertySourceData);
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Considering this ConfigMap as containing values from a single file");
            }
            String extension = getExtension(entry.getKey()).get();
            PropertySource propertySource = PROPERTY_SOURCE_READERS.stream()
                    .filter(reader -> reader.getExtensions().contains(extension))
                    .map(reader -> reader.read(entry.getKey(), entry.getValue().getBytes()))
                    .peek(map -> map.putIfAbsent(CONFIG_MAP_RESOURCE_VERSION, configMap.getMetadata().getResourceVersion()))
                    .map(map -> PropertySource.of(entry.getKey() + KUBERNETES_CONFIG_MAP_NAME_SUFFIX, map))
                    .findFirst()
                    .orElse(PropertySource.of(Collections.emptyMap()));

            addPropertySourceToCache(propertySource);

            return propertySource;
        }
    }

    /**
     * Adds the given {@link PropertySource} to the cache.
     *
     * @param propertySource The property source to add
     */
    static void addPropertySourceToCache(PropertySource propertySource) {
        propertySources.put(propertySource.getName(), propertySource);
    }

    /**
     * Removes the given {@link PropertySource} name from the cache.
     *
     * @param name The property source name
     */
    static void removePropertySourceFromCache(String name) {
        propertySources.remove(name);
    }

    /**
     * @return the property source cache.
     */
    static Map<String, PropertySource> getPropertySourceCache() {
        return propertySources;
    }

    private static String getPropertySourceName(ConfigMap configMap) {
        return configMap.getMetadata().getName() + KUBERNETES_CONFIG_MAP_NAME_SUFFIX;
    }

    private static Optional<String> getExtension(String filename) {
        return Optional.of(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    private Flowable<PropertySource> getPropertySourcesFromConfigMaps() {
        Collection<String> includes = configuration.getConfigMaps().getIncludes();
        Collection<String> excludes = configuration.getConfigMaps().getExcludes();
        Predicate<ConfigMap> includesFilter = configMap -> true;
        Predicate<ConfigMap> excludesFilter = configMap -> true;

        if (!includes.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("ConfigMap includes: {}", includes);
            }
            includesFilter = configMap -> includes.contains(configMap.getMetadata().getName());
        }

        if (!excludes.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("ConfigMap excludes: {}", excludes);
            }
            excludesFilter = configMap -> !excludes.contains(configMap.getMetadata().getName());
        }

        Map<String, String> labels = configuration.getConfigMaps().getLabels();
        String labelSelector = computeLabelSelector(labels);

        return Flowable.fromPublisher(client.listConfigMaps(configuration.getNamespace(), labelSelector))
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
                .map(KubernetesConfigurationClient::configMapAsPropertySource);
    }

    private Flowable<PropertySource> getPropertySourcesFromSecrets() {
        if (configuration.getSecrets().isEnabled()) {
            Collection<String> includes = configuration.getSecrets().getIncludes();
            Collection<String> excludes = configuration.getSecrets().getExcludes();
            Predicate<Secret> includesFilter = s -> true;
            Predicate<Secret> excludesFilter = s -> true;

            if (!includes.isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Secret includes: {}", includes);
                }
                includesFilter = s -> includes.contains(s.getMetadata().getName());
            }

            if (!excludes.isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Secret excludes: {}", excludes);
                }
                excludesFilter = s -> !excludes.contains(s.getMetadata().getName());
            }

            Map<String, String> labels = configuration.getSecrets().getLabels();
            String labelSelector = computeLabelSelector(labels);

            return Flowable.fromPublisher(client.listSecrets(configuration.getNamespace(), labelSelector))
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
                    .map(this::secretAsPropertySource);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Kubernetes secrets access is disabled");
            }
            return Flowable.empty();
        }
    }

    static String computeLabelSelector(Map<String, String> labels) {
        String labelSelector = null;
        if (!labels.isEmpty()) {
            labelSelector = labels.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(","));

            if (LOG.isTraceEnabled()) {
                LOG.trace("labelSelector: {}", labelSelector);
            }
        }
        return labelSelector;
    }

    private PropertySource secretAsPropertySource(Secret secret) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing PropertySources for Secret: {}", secret);
        }
        String name = secret.getMetadata().getName() + KUBERNETES_SECRET_NAME_SUFFIX;
        Map<String, String> data = secret.getData();
        Map<String, Object> propertySourceData = data.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> decodeSecret(e.getValue())));
        PropertySource propertySource = PropertySource.of(name, propertySourceData);
        addPropertySourceToCache(propertySource);
        return propertySource;
    }

    private String decodeSecret(String secretValue) {
        return new String(Base64.getDecoder().decode(secretValue));
    }

}
