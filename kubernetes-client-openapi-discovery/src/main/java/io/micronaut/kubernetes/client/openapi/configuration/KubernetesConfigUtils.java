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

import io.micronaut.context.env.EnvironmentPropertySource;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.env.PropertySourceReader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.health.KubernetesHealthIndicator;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import io.micronaut.kubernetes.client.openapi.util.KubernetesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Utility methods for Kubernetes Configuration client.
 */
final class KubernetesConfigUtils {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesUtils.class);

    private static final String ENV_KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";

    private static final List<PropertySourceReader> PROPERTY_SOURCE_READERS = StreamSupport.stream(ServiceLoader.load(PropertySourceLoader.class).spliterator(), false)
        .collect(Collectors.toList());

    /**
     * Converts a {@link V1ConfigMap} into a {@link PropertySource}.
     *
     * @param configMap the {@link V1ConfigMap} instance
     * @return {@link PropertySource} instance
     */
    static PropertySource configMapAsPropertySource(V1ConfigMap configMap) {
        LOG.trace("Creating PropertySource for ConfigMap: {}", configMap);
        String name = getPropertySourceName(configMap);
        Map<String, String> data = configMap.getData();
        if (CollectionUtils.isEmpty(data)) {
            return PropertySource.of(Collections.emptyMap());
        }

        Map.Entry<String, String> entry = data.entrySet().iterator().next();
        if (data.size() > 1 || getExtension(entry.getKey()).isEmpty()) {
            LOG.trace("Considering this ConfigMap as containing multiple literal key/values");
            data.putIfAbsent(KubernetesConfigurationClient.CONFIG_MAP_RESOURCE_VERSION, configMap.getMetadata().getResourceVersion());
            Map<String, Object> propertySourceData = new HashMap<>(data);
            return PropertySource.of(name, propertySourceData);
        } else {
            LOG.trace("Considering this ConfigMap as containing values from a single file");
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
     * Converts config map mounted as volume into a {@link PropertySource}.
     *
     * @param mountPoint the mount point
     * @param data       the configmaps data in the mounted volume where keys are file names and values is the file content
     * @return list of {@link PropertySource} instances
     */
    static List<PropertySource> configMapAsPropertySource(String mountPoint, Map<String, String> data) {
        LOG.trace("Creating {} PropertySources for ConfigMaps mounted at: {}", data.size(), mountPoint);
        if (CollectionUtils.isEmpty(data)) {
            return Collections.singletonList(PropertySource.of(Collections.emptyMap()));
        }

        List<PropertySource> propertySources = new ArrayList<>(data.size());

        for (Map.Entry<String, String> entry : data.entrySet()) {
            Optional<String> extension = getExtension(entry.getKey());
            if (extension.isEmpty()) {
                LOG.info("Failed to deduce the extension for file: {}", entry.getKey());
                continue;
            }

            String fileExtension = extension.get();
            String propertyName = mountPoint + "/" + entry.getKey() + KubernetesConfigurationClient.KUBERNETES_CONFIG_MAP_NAME_SUFFIX;

            int priority = EnvironmentPropertySource.POSITION + 150;
            PropertySource propertySource = PROPERTY_SOURCE_READERS.stream()
                .filter(reader -> reader.getExtensions().contains(fileExtension))
                .map(reader -> reader.read(entry.getKey(), entry.getValue().getBytes()))
                .map(map -> PropertySource.of(propertyName, map, priority))
                .findFirst()
                .orElse(PropertySource.of(Collections.emptyMap()));
            propertySources.add(propertySource);
        }

        return propertySources;
    }

    /**
     * Converts a {@link V1Secret} into a {@link PropertySource}.
     *
     * @param secret the {@link V1Secret} instance
     * @return {@link PropertySource} instance
     */
    static PropertySource secretAsPropertySource(V1Secret secret) {
        LOG.trace("Creating PropertySource for Secret: {}", secret);
        String name = secret.getMetadata().getName() + KubernetesConfigurationClient.KUBERNETES_SECRET_NAME_SUFFIX;
        Map<String, byte[]> data = secret.getData();
        Map<String, Object> propertySourceData;
        if (data != null) {
            propertySourceData = data.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> new String(v.getValue())));
        } else {
            propertySourceData = Collections.emptyMap();
        }
        int priority = EnvironmentPropertySource.POSITION + 100;
        PropertySource propertySource = PropertySource.of(name, propertySourceData, priority);
        KubernetesConfigurationClient.addPropertySourceToCache(propertySource);
        return propertySource;
    }

    /**
     * Creates a label selector filter from pod and passed labels.
     *
     * @param client                      the client
     * @param podLabelKeys                the list of label keys inside a pod
     * @param namespace                   the namespace
     * @param labels                      the additional labels
     * @param exceptionOnPodLabelsMissing should an exception be thrown if configured pod label key is not found in pod labels
     * @return the label selector filter
     */
    static Mono<String> computePodLabelSelector(CoreV1ApiReactor client, List<String> podLabelKeys,
                                                       String namespace, Map<String, String> labels,
                                                       boolean exceptionOnPodLabelsMissing) {
        // determine if we are running inside a pod. This environment variable is always been set.
        String host = System.getenv(ENV_KUBERNETES_SERVICE_HOST);
        if (StringUtils.isEmpty(host) || CollectionUtils.isEmpty(podLabelKeys)) {
            return Mono.just(computeLabelSelector(labels));
        }

        String podName = System.getenv(KubernetesHealthIndicator.HOSTNAME_ENV_VARIABLE);
        return client.readNamespacedPod(podName, namespace, null)
            .doOnError(throwable -> LOG.error("Failed to read the Pod [{}] the application is running in", podName, throwable))
            .map(pod -> {
                Map<String, String> result = new HashMap<>();
                Map<String, String> podLabels = pod.getMetadata().getLabels();
                for (String key : podLabelKeys) {
                    String value = podLabels.get(key);
                    if (value != null) {
                        result.put(key, value);
                        LOG.trace("Including pod label: {}={}", key, value);
                    } else {
                        LOG.warn("Pod metadata does not contain label: {}", key);
                        if (exceptionOnPodLabelsMissing) {
                            throw new ConfigurationException("Pod metadata does not contain label [" +
                                key + "] and the exception-on-pod-labels-missing property is set");
                        }
                    }
                }
                LOG.debug("Computed pod label selectors {}", result);
                result.putAll(labels);
                return computeLabelSelector(result);
            })
            .doOnError(throwable -> LOG.error("Failed to compute the label selector {} from the Pod [{}]", podLabelKeys, podName, throwable));
    }

    /**
     * Creates a label selector filter based on the passed labels.
     *
     * @param labels the map of labels
     * @return the label selector filter
     */
    private static String computeLabelSelector(Map<String, String> labels) {
        if (CollectionUtils.isEmpty(labels)) {
            return StringUtils.EMPTY_STRING;
        }
        String labelSelector = labels.entrySet()
            .stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(","));
        LOG.trace("labelSelector: {}", labelSelector);
        return labelSelector;
    }

    private static String getPropertySourceName(V1ConfigMap configMap) {
        return configMap.getMetadata().getName() + KubernetesConfigurationClient.KUBERNETES_CONFIG_MAP_NAME_SUFFIX;
    }

    private static Optional<String> getExtension(String filename) {
        return Optional.of(filename)
            .filter(f -> f.contains("."))
            .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }
}
