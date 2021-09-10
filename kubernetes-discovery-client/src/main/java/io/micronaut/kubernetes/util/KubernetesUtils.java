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

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Secret;
import io.micronaut.context.env.EnvironmentPropertySource;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceReader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.jackson.env.JsonPropertySourceLoader;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.kubernetes.configuration.KubernetesConfigurationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.micronaut.kubernetes.health.KubernetesHealthIndicator.HOSTNAME_ENV_VARIABLE;

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
     * Converts a {@link V1ConfigMap} into a {@link PropertySource}.
     *
     * @param configMap the ConfigMap
     * @return A PropertySource
     */
    public static PropertySource configMapAsPropertySource(V1ConfigMap configMap) {
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
     * @param secret The {@link V1Secret} to transform
     * @return The converted {@link PropertySource}.
     */
    public static PropertySource secretAsPropertySource(V1Secret secret) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing PropertySources for Secret: {}", secret);
        }
        String name = secret.getMetadata().getName() + KubernetesConfigurationClient.KUBERNETES_SECRET_NAME_SUFFIX;
        Map<String, byte[]> data = secret.getData();
        Map<String, Object> propertySourceData = null;
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
     * @param includes the objects to include
     * @return a {@link Predicate} based on a collection of object names to include
     */
    public static Predicate<KubernetesObject> getIncludesFilter(Collection<String> includes) {
        Predicate<KubernetesObject> includesFilter = s -> true;

        if (!includes.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Includes: {}", includes);
            }
            includesFilter = s -> {
                boolean result = includes.contains(s.getMetadata().getName());
                if (LOG.isTraceEnabled()) {
                    if (result) {
                        LOG.trace("Includes filter matched: {}", s.getMetadata().getName());
                    } else {
                        LOG.trace("Includes filter not-matched: {}", s.getMetadata().getName());
                    }
                }
                return result;
            };
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
            excludesFilter = s -> {
                boolean result = !excludes.contains(s.getMetadata().getName());
                if (LOG.isTraceEnabled()) {
                    if (result) {
                        LOG.trace("Excludes matched: {}", s.getMetadata().getName());
                    } else {
                        LOG.trace("Excludes filter not-matched: {}", s.getMetadata().getName());
                    }
                }
                return result;
            };
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
                boolean result = labels.entrySet().stream().allMatch(
                        e -> kubernetesObjectLabels.containsKey(e.getKey()) && kubernetesObjectLabels.get(e.getKey()).equals(e.getValue()));
                if (LOG.isTraceEnabled()) {
                    if (result) {
                        LOG.trace("Filter labels filter matched: {}", kubernetesObject.getMetadata().getName());
                    } else {
                        LOG.trace("Filter labels not-matched: {}", kubernetesObject.getMetadata().getName());
                    }
                }
                return result;
            };
        }
        return labelsFilter;
    }

    /**
     * @param client       the {@link CoreV1ApiReactorClient}
     * @param podLabelKeys the list of labels inside a pod
     * @param namespace    in the configuration
     * @param labels       the labels
     * @param failFast     should and exception be thrown if configured pod label is not found
     * @return the filtered labels of the current pod
     */
    public static Mono<String> computePodLabelSelector(CoreV1ApiReactorClient client, List<String> podLabelKeys,
                                                       String namespace, Map<String, String> labels,
                                                       boolean failFast) {
        // determine if we are running inside a pod. This environment variable is always been set.
        String host = System.getenv(ENV_KUBERNETES_SERVICE_HOST);
        if (host == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Not running on k8s");
            }
            return Mono.just(computeLabelSelector(labels));
        }

        final String podName = System.getenv(HOSTNAME_ENV_VARIABLE);
        return client.readNamespacedPod(podName, namespace, null, null, null)
                .doOnError(ApiException.class, throwable ->
                        LOG.error("Failed to read the Pod [" + podName + "] the application is running in: " + throwable.getResponseBody(), throwable))
                .map(pod -> {
                    Map<String, String> result = new HashMap<>();
                    Map<String, String> podLabels = Objects.requireNonNull(pod.getMetadata()).getLabels();
                    for (String key : podLabelKeys) {
                        String value = podLabels.get(key);
                        if (value != null) {
                            result.put(key, value);
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Including pod label: {}={}", key, value);
                            }
                        } else {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("Pod metadata does not contain label: {}", key);
                            }
                            if (failFast) {
                                throw new ConfigurationException("Pod metadata does not contain label: " + key +
                                                                    " and the fail fast property is set");
                            }
                        }
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Computed pod label selectors {}", result);
                    }
                    result.putAll(labels);
                    return computeLabelSelector(result);
                })
                .doOnError(throwable -> LOG.error("Failed to compute the label selector [" + podLabelKeys + "] from the Pod [" + podName + "]: " + throwable.getMessage(), throwable));
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
