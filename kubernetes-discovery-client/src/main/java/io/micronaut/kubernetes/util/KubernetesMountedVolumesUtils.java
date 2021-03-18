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
import io.micronaut.kubernetes.configuration.KubernetesMountedVolumeConfigurationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class with methods to help with ConfigMaps and Secrets read from mounted volumes
 *
 * @author Stephane Paulus
 * @since 1.0.0
 */
public class KubernetesMountedVolumesUtils {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesMountedVolumesUtils.class);
    private static final List<PropertySourceReader> PROPERTY_SOURCE_READERS = Arrays.asList(
            new YamlPropertySourceLoader(),
            new JsonPropertySourceLoader(),
            new PropertiesPropertySourceLoader());

    /**
     * Converts a {@link String} and a {@link Map<String,String>}  into a {@link PropertySource}.
     *
     * @param key the key
     * @param data the configmaps data
     * @return A PropertySource
     */
    public static PropertySource configMapAsPropertySource(String key, Map<String, String> data) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing PropertySources for ConfigMap: {}", key);
        }
        String name = key + KubernetesMountedVolumeConfigurationClient.KUBERNETES_CONFIG_MAP_NAME_SUFFIX;

        if (data == null || data.isEmpty()) {
            return PropertySource.of(Collections.emptyMap());
        }

        Map.Entry<String, String> entry = data.entrySet().iterator().next();
        if (data.size() > 1 || !getExtension(entry.getKey()).isPresent()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Considering this ConfigMap as containing multiple literal key/values");
            }
            Map<String, Object> propertySourceData = new HashMap<>(data);
            return PropertySource.of(name, propertySourceData);
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Considering this ConfigMap as containing values from a single file");
            }
            String extension = getExtension(entry.getKey()).get();
            int priority = EnvironmentPropertySource.POSITION + 150;
            PropertySource propertySource = PROPERTY_SOURCE_READERS.stream()
                    .filter(reader -> reader.getExtensions().contains(extension))
                    .map(reader -> reader.read(entry.getKey(), entry.getValue().getBytes()))
                    .map(map -> PropertySource.of(entry.getKey() + KubernetesMountedVolumeConfigurationClient.KUBERNETES_CONFIG_MAP_NAME_SUFFIX, map, priority))
                    .findFirst()
                    .orElse(PropertySource.of(Collections.emptyMap()));

            KubernetesMountedVolumeConfigurationClient.addPropertySourceToCache(propertySource);

            return propertySource;
        }
    }

    private static Optional<String> getExtension(String filename) {
        return Optional.of(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }
}
