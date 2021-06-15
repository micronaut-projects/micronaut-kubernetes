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
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
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

import static io.micronaut.kubernetes.util.KubernetesMountedVolumesUtils.configMapAsPropertySource;


/**
 * A {@link ConfigurationClient} implementation that provides {@link PropertySource}s read from Kubernetes ConfigMap's.
 *
 * @author Stephane Paulus
 * @since 1.0.0
 */
@Singleton
@Requires(property = ConfigurationClient.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@BootstrapContextCompatible
public class KubernetesMountedVolumeConfigurationClient implements ConfigurationClient {

    public static final String KUBERNETES_SECRET_NAME_SUFFIX = " (Kubernetes Secret)";
    public static final String KUBERNETES_CONFIG_MAP_NAME_SUFFIX = " (Kubernetes ConfigMap)";

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesMountedVolumeConfigurationClient.class);

    private static Map<String, PropertySource> propertySources = new ConcurrentHashMap<>();

    private final KubernetesConfiguration configuration;

    /**
     * @param configuration The configuration properties
     */
    public KubernetesMountedVolumeConfigurationClient(KubernetesConfiguration configuration) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing {}", getClass().getName());
        }
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

        Flowable<PropertySource> propertySourceFlowable = Flowable.empty();
        if (configuration.getConfigMaps().isEnabled()) {
            Collection<String> mountedVolumePaths = configuration.getConfigMaps().getPaths();
            if (!mountedVolumePaths.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading configmap from the following mounted volumes: {}", mountedVolumePaths);
                }

                List<PropertySource> propertySources = new ArrayList<>();
                mountedVolumePaths.stream()
                        .map(Paths::get)
                        .forEach(path -> {
                            LOG.trace("Processing path: {}", path);
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                                for (Path file : stream) {
                                    if (!Files.isDirectory(file)) {
                                        String key = file.getFileName().toString();
                                        String value = new String(Files.readAllBytes(file));
                                        if (LOG.isTraceEnabled()) {
                                            LOG.trace("Processing key: {}", key);
                                        }
                                        final HashMap<String, String> objectObjectHashMap = new HashMap();
                                        objectObjectHashMap.put(key, value);
                                        final PropertySource propertySource = configMapAsPropertySource(key, objectObjectHashMap);
                                        addPropertySourceToCache(propertySource);
                                        propertySources.add(propertySource);
                                    }
                                }
                            } catch (IOException e) {
                                LOG.warn("Exception occurred when reading configmap from path: {}", path);
                                LOG.warn(e.getMessage(), e);
                            }
                        });

                propertySourceFlowable = propertySourceFlowable.mergeWith(Flowable.fromIterable(propertySources));
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Kubernetes configmap access is disabled");
            }
        }
        return propertySourceFlowable;
    }

    private Flowable<PropertySource> getPropertySourcesFromSecrets() {
        Flowable<PropertySource> propertySourceFlowable = Flowable.empty();
        if (configuration.getSecrets().isEnabled()) {
            Collection<String> mountedVolumePaths = configuration.getSecrets().getPaths();
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
