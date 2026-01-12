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

import io.micronaut.context.env.EmptyPropertySource;
import io.micronaut.context.env.EnvironmentPropertySource;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.common.KubernetesListObject;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import io.micronaut.kubernetes.client.openapi.util.KubernetesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility methods for Kubernetes Configuration client.
 */
final class KubernetesConfigUtils {

    static final int API_PROPERTY_SOURCE_PRIORITY = EnvironmentPropertySource.POSITION + 100;
    static final int MOUNTED_FILE_PROPERTY_SOURCE_PRIORITY = EnvironmentPropertySource.POSITION + 150;

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesUtils.class);

    private static final String ENV_KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";

    private static final String PROPERTY_SOURCE_NAME_TEMPLATE = "%s (Kubernetes %s)";
    private static final String OBJECT_RES_VERSION_PROP_NAME_TEMPLATE = "%s.%s.resource-version";
    private static final String LIST_RES_VERSION_PROP_NAME_TEMPLATE = "%s.resource-version";

    /**
     * Creates a property source from given kubernetes list object.
     *
     * @param kubernetesListObject the kubernetes list object
     * @return property source
     */
    static PropertySource kubernetesListAsPropertySource(KubernetesListObject kubernetesListObject) {
        if (CollectionUtils.isEmpty(kubernetesListObject.getItems())) {
            return new EmptyPropertySource();
        }
        String objectType = kubernetesListObject.getClass().getSimpleName();
        String resVersionPropertyName = LIST_RES_VERSION_PROP_NAME_TEMPLATE.formatted(objectType.toLowerCase());
        String resVersionPropertyValue = kubernetesListObject.getMetadata().getResourceVersion();
        LOG.trace("Creating PropertySource with resourceVersion={} for {}", resVersionPropertyValue, objectType);
        return PropertySource.of("Kubernetes " + objectType,
            Collections.singletonMap(resVersionPropertyName, resVersionPropertyValue),
            API_PROPERTY_SOURCE_PRIORITY);
    }

    /**
     * Creates a property name for property which contains resource version of given kubernetes object.
     *
     * @param kubernetesObject the kubernetes object
     * @return property name
     */
    static String createResVersionPropertyName(KubernetesObject kubernetesObject) {
        String objectType = kubernetesObject.getClass().getSimpleName();
        String objectName = kubernetesObject.getMetadata().getName();
        return OBJECT_RES_VERSION_PROP_NAME_TEMPLATE.formatted(objectType.toLowerCase(), objectName);
    }

    /**
     * Creates a property source name from given kubernetes object.
     *
     * @param kubernetesObject the kubernetes object
     * @return property source name
     */
    static String createPropertySourceName(KubernetesObject kubernetesObject) {
        String objectName = kubernetesObject.getMetadata().getName();
        String objectType = kubernetesObject.getClass().getSimpleName();
        return PROPERTY_SOURCE_NAME_TEMPLATE.formatted(objectName, objectType);
    }

    /**
     * Creates a property source name from given file path.
     *
     * @param filePath the file path
     * @param type the kubernetes type
     * @return property source name
     */
    static String createPropertySourceName(String filePath, Class<? extends KubernetesObject> type) {
        return PROPERTY_SOURCE_NAME_TEMPLATE.formatted(filePath, type.getSimpleName());
    }

    /**
     * Converts a {@link V1ConfigMap} into a {@link PropertySource}.
     *
     * @param configMap             the {@link V1ConfigMap} instance
     * @param propertySourceLoaders the collection of property source loaders
     * @return {@link PropertySource} instance
     */
    static PropertySource configMapAsPropertySource(V1ConfigMap configMap, Collection<PropertySourceLoader> propertySourceLoaders) {
        LOG.trace("Creating PropertySource for ConfigMap: {}", configMap.getMetadata().getName());
        Map<String, String> data = configMap.getData();
        if (CollectionUtils.isEmpty(data)) {
            return new EmptyPropertySource();
        }

        Map<String, Object> propertySourceData;
        Map.Entry<String, String> entry = data.entrySet().iterator().next();
        Optional<String> extensionOpt = getExtension(entry.getKey());
        if (data.size() > 1 || extensionOpt.isEmpty()) {
            LOG.trace("Considering this ConfigMap as containing multiple literal key/values");
            propertySourceData = new HashMap<>(data);
        } else {
            LOG.trace("Considering this ConfigMap as containing values from a single file");
            String extension = extensionOpt.get();
            Optional<PropertySourceLoader> propertySourceLoader = propertySourceLoaders.stream()
                .filter(loader -> loader.getExtensions().contains(extension))
                .findFirst();
            if (propertySourceLoader.isEmpty()) {
                Set<String> propertySourceExtensions = propertySourceLoader.stream()
                    .flatMap(loader -> loader.getExtensions().stream())
                    .collect(Collectors.toSet());
                LOG.info("Could not find property source loader for extension '{}' from ConfigMap '{}'. Supported extensions: {}",
                    extension, configMap.getMetadata().getName(), propertySourceExtensions);
                propertySourceData = Collections.emptyMap();
            } else {
                propertySourceData = propertySourceLoader.get().read(entry.getKey(), entry.getValue().getBytes());
            }
        }

        if (propertySourceData.isEmpty()) {
            return new EmptyPropertySource();
        } else {
            String propertySourceName = createPropertySourceName(configMap);
            String resVersionPropertyName = createResVersionPropertyName(configMap);
            String resVersionPropertyValue = configMap.getMetadata().getResourceVersion();
            propertySourceData.put(resVersionPropertyName, resVersionPropertyValue);
            return PropertySource.of(propertySourceName, propertySourceData, API_PROPERTY_SOURCE_PRIORITY);
        }
    }

    /**
     * Converts config map mounted as volume into a {@link PropertySource}.
     *
     * @param mountPoint            the mount point
     * @param data                  the configmaps data in the mounted volume where keys are file names and values is the file content
     * @param propertySourceLoaders the collection of property source loaders
     * @return list of {@link PropertySource} instances
     */
    static List<PropertySource> configMapAsPropertySource(String mountPoint, Map<String, String> data, Collection<PropertySourceLoader> propertySourceLoaders) {
        List<PropertySource> propertySources = new ArrayList<>(data.size());

        data.forEach((fileName, fileContent) -> {
            LOG.trace("Creating PropertySource for ConfigMap from file: {}", fileName);
            Optional<String> extensionOpt = getExtension(fileName);
            if (extensionOpt.isEmpty()) {
                LOG.info("Failed to deduce the extension for file: {}", fileName);
                return;
            }

            String extension = extensionOpt.get();
            Optional<PropertySourceLoader> propertySourceLoader = propertySourceLoaders.stream()
                .filter(loader -> loader.getExtensions().contains(extension))
                .findFirst();
            if (propertySourceLoader.isEmpty()) {
                Set<String> propertySourceExtensions = propertySourceLoaders.stream()
                    .flatMap(loader -> loader.getExtensions().stream())
                    .collect(Collectors.toSet());
                LOG.info("Could not find property source loader for extension '{}' from file '{}'. Supported extensions: {}",
                    extension, fileName, propertySourceExtensions);
            } else {
                String propertySourceName = createPropertySourceName(mountPoint + "/" + fileName, V1ConfigMap.class);
                Map<String, Object> propertySourceData = propertySourceLoader.get().read(fileName, fileContent.getBytes());
                propertySources.add(PropertySource.of(propertySourceName, propertySourceData, MOUNTED_FILE_PROPERTY_SOURCE_PRIORITY));
            }
        });

        return propertySources;
    }

    /**
     * Converts a {@link V1Secret} into a {@link PropertySource}.
     *
     * @param secret the {@link V1Secret} instance
     * @return {@link PropertySource} instance
     */
    static PropertySource secretAsPropertySource(V1Secret secret) {
        LOG.trace("Creating PropertySource for Secret: {}", secret.getMetadata().getName());
        Map<String, byte[]> data = secret.getData();
        if (data == null) {
            return new EmptyPropertySource();
        }
        Map<String, Object> propertySourceData = data.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, v -> new String(v.getValue())));
        String resVersionPropertyName = createResVersionPropertyName(secret);
        String resVersionPropertyValue = secret.getMetadata().getResourceVersion();
        propertySourceData.put(resVersionPropertyName, resVersionPropertyValue);
        String propertySourceName = createPropertySourceName(secret);
        return PropertySource.of(propertySourceName, propertySourceData, API_PROPERTY_SOURCE_PRIORITY);
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

        String podName = System.getenv(KubernetesConfiguration.HOSTNAME_ENV_VARIABLE);
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

    private static Optional<String> getExtension(String filename) {
        return Optional.of(filename)
            .filter(f -> f.contains("."))
            .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }
}
