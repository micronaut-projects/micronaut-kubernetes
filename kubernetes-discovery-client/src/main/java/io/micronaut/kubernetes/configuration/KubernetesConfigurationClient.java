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
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
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
            return PROPERTY_SOURCE_READERS.stream()
                    .filter(reader -> reader.getExtensions().contains(extension))
                    .map(reader -> reader.read(entry.getKey(), entry.getValue().getBytes()))
                    .peek(map -> map.putIfAbsent(CONFIG_MAP_RESOURCE_VERSION, configMap.getMetadata().getResourceVersion()))
                    .map(map -> PropertySource.of(entry.getKey() + KUBERNETES_CONFIG_MAP_NAME_SUFFIX, map))
                    .findFirst()
                    .orElse(PropertySource.of(Collections.emptyMap()));
        }
    }

    private static String getPropertySourceName(ConfigMap configMap) {
        return configMap.getMetadata().getName() + KUBERNETES_CONFIG_MAP_NAME_SUFFIX;
    }

    private static Optional<String> getExtension(String filename) {
        return Optional.of(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    /**
     * Retrieves all of the {@link PropertySource} registrations for the given environment.
     *
     * @param environment The environment
     * @return A {@link Publisher} that emits zero or many {@link PropertySource} instances discovered for the given environment
     */
    @Override
    public Publisher<PropertySource> getPropertySources(Environment environment) {
        return getPropertySourcesFromConfigMaps().mergeWith(getPropertySourcesFromSecrets());
    }

    private Flowable<PropertySource> getPropertySourcesFromConfigMaps() {
        return Flowable.fromPublisher(client.listConfigMaps(configuration.getNamespace()))
                .doOnError(throwable -> LOG.error("Error while trying to list all Kubernetes ConfigMaps in the namespace [" + configuration.getNamespace() + "]", throwable))
                .onErrorReturn(throwable -> new ConfigMapList())
                .doOnNext(configMapList -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found {} config maps", configMapList.getItems().size());
                    }
                })
                .flatMapIterable(ConfigMapList::getItems)
                .doOnNext(configMap -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Adding config map with name {}", configMap.getMetadata().getName());
                    }
                })
                .map(KubernetesConfigurationClient::configMapAsPropertySource);
    }

    private Flowable<PropertySource> getPropertySourcesFromSecrets() {
        return Flowable.fromPublisher(client.listSecrets(configuration.getNamespace()))
                .doOnError(throwable -> LOG.error("Error while trying to list all Kubernetes Secrets in the namespace [" + configuration.getNamespace() + "]", throwable))
                .onErrorReturn(throwable -> new SecretList())
                .doOnNext(secretList -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found {} secrets. Filtering Opaque secrets", secretList.getItems().size());
                    }
                })
                .flatMapIterable(SecretList::getItems)
                .filter(secret -> secret.getType().equals(OPAQUE_SECRET_TYPE))
                .doOnNext(secret -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Adding secret with name {}", secret.getMetadata().getName());
                    }
                })
                .map(this::secretAsPropertySource);
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

    private PropertySource secretAsPropertySource(Secret secret) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing PropertySources for Secret: {}", secret);
        }
        String name = secret.getMetadata().getName() + KUBERNETES_SECRET_NAME_SUFFIX;
        Map<String, String> data = secret.getData();
        Map<String, Object> propertySourceData = data.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> decodeSecret(e.getValue())));
        return PropertySource.of(name, propertySourceData);
    }

    private String decodeSecret(String secretValue) {
        return new String(Base64.getDecoder().decode(secretValue));
    }

}
