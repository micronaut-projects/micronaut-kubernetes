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
package io.micronaut.kubernetes.util;

import io.micronaut.context.env.EnvironmentPropertySource;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceReader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.jackson.env.JsonPropertySourceLoader;
import io.micronaut.kubernetes.client.v1.KubernetesObject;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.micronaut.kubernetes.configuration.KubernetesConfigurationClient;
import io.reactivex.functions.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class with methods to help with ConfigMaps and Secrets.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class KubernetesUtils {

    public static final String ENV_KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesUtils.class);
    private static final List<PropertySourceReader> PROPERTY_SOURCE_READERS = Arrays.asList(
            new YamlPropertySourceLoader(),
            new JsonPropertySourceLoader(),
            new PropertiesPropertySourceLoader());

    /**
     * Converts a {@link ConfigMap} into a {@link PropertySource}.
     *
     * @param configMap the ConfigMap
     * @return A PropertySource
     */
    public static PropertySource configMapAsPropertySource(ConfigMap configMap) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing PropertySources for ConfigMap: {}", configMap);
        }
        String name = getPropertySourceName(configMap);
        Map<String, String> data = configMap.getData();

        if (data == null || data.isEmpty()) {
            return PropertySource.of(Collections.emptyMap());
        }

        Map.Entry<String, String> entry = data.entrySet().iterator().next();
        if (data.size() > 1 || !getExtension(entry.getKey()).isPresent()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Considering this ConfigMap as containing multiple literal key/values");
            }
            data.putIfAbsent(KubernetesConfigurationClient.CONFIG_MAP_RESOURCE_VERSION, configMap.getMetadata().getResourceVersion());
            Map<String, Object> propertySourceData = new HashMap<>(data);
            return PropertySource.of(name, propertySourceData);
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Considering this ConfigMap as containing values from a single file");
            }
            String extension = getExtension(entry.getKey()).get();
            int priority = EnvironmentPropertySource.POSITION + 100;
            PropertySource propertySource = PROPERTY_SOURCE_READERS.stream()
                    .filter(reader -> reader.getExtensions().contains(extension))
                    .map(reader -> reader.read(entry.getKey(), entry.getValue().getBytes()))
                    .peek(map -> map.putIfAbsent(KubernetesConfigurationClient.CONFIG_MAP_RESOURCE_VERSION, configMap.getMetadata().getResourceVersion()))
                    .map(map -> PropertySource.of(entry.getKey() + KubernetesConfigurationClient.KUBERNETES_CONFIG_MAP_NAME_SUFFIX, map, priority))
                    .findFirst()
                    .orElse(PropertySource.of(Collections.emptyMap()));

            KubernetesConfigurationClient.addPropertySourceToCache(propertySource);

            return propertySource;
        }
    }

    /**
     * Determines the value of a Kubernetes labelSelector filter based on the passed labels.
     *
     * @param labels the labels
     * @return the value of the labelSelector filter
     */
    public static String computeLabelSelector(Map<String, String> labels) {
        String labelSelector = "";
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

    /**
     * @param secret The {@link Secret} to transform
     * @return The converted {@link PropertySource}.
     */
    public static PropertySource secretAsPropertySource(Secret secret) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing PropertySources for Secret: {}", secret);
        }
        String name = secret.getMetadata().getName() + KubernetesConfigurationClient.KUBERNETES_SECRET_NAME_SUFFIX;
        Map<String, String> data = secret.getStringData();
        Map<String, Object> propertySourceData = data.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        int priority = EnvironmentPropertySource.POSITION + 100;
        PropertySource propertySource = PropertySource.of(name, propertySourceData, priority);
        KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
        return propertySource;
    }

    /**
     * @param includes the objects to include
     * @return a {@link Predicate} based on a collection of object names to include
     */
    public static Predicate<KubernetesObject> getIncludesFilter(Collection<String> includes) {
        Predicate<KubernetesObject> includesFilter = s -> true;

        if (!includes.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Includes: {}", includes);
            }
            includesFilter = s -> includes.contains(s.getMetadata().getName());
        }

        return includesFilter;
    }

    /**
     * @param excludes the objects to excludes
     * @return a {@link Predicate} based on a collection of object names to excludes
     */
    public static Predicate<KubernetesObject> getExcludesFilter(Collection<String> excludes) {
        Predicate<KubernetesObject> excludesFilter = s -> true;

        if (!excludes.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Excludes: {}", excludes);
            }
            excludesFilter = s -> !excludes.contains(s.getMetadata().getName());
        }

        return excludesFilter;
    }

    /**
     * @param labels the labels to include
     * @return a {@link Predicate} based on labels the kubernetes objects has to match to return true
     */
    public static Predicate<KubernetesObject> getLabelsFilter(Map<String, String> labels) {
        Predicate<KubernetesObject> labelsFilter = s -> true;
        if (!labels.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Filter labels: {}", labels.keySet());
            }
            labelsFilter = kubernetesObject -> {
                Map<String, String> kubernetesObjectLabels = kubernetesObject.getMetadata().getLabels();
                return labels.entrySet().stream().allMatch(
                        e -> kubernetesObjectLabels.containsKey(e.getKey()) && kubernetesObjectLabels.get(e.getKey()).equals(e.getValue()));
            };
        }
        return labelsFilter;
    }

    /**
     * Utility method that evaluates whether the application is running inside K8s POD.
     *
     * @return {@code true} if runs in Kubernetes otherwise {@code false}.
     */
    public static boolean isRunningInKubernetes() {
        // determine if we are running inside a pod. This environment variable is always been set.
        String host = System.getenv(ENV_KUBERNETES_SERVICE_HOST);
        return host == null;
    }

    private static String getPropertySourceName(ConfigMap configMap) {
        return configMap.getMetadata().getName() + KubernetesConfigurationClient.KUBERNETES_CONFIG_MAP_NAME_SUFFIX;
    }

    private static Optional<String> getExtension(String filename) {
        return Optional.of(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }
}
