/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.kubernetes.client.openapi.config;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.ResourceResolver;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract implementation of {@link KubeConfigLoader} which provides common methods for loading kube config.
 */
@Internal
public abstract class AbstractKubeConfigLoader implements KubeConfigLoader {

    private final ResourceResolver resourceResolver;

    private KubeConfig kubeConfig;

    protected AbstractKubeConfigLoader(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public KubeConfig getKubeConfig() {
        if (kubeConfig == null) {
            kubeConfig = loadKubeConfig();
        }
        return kubeConfig;
    }

    protected abstract KubeConfig loadKubeConfig();

    /**
     * Loads kube config from the file on given path.
     *
     * @param filePath the file path
     * @return instance of {@link KubeConfig}
     */
    protected KubeConfig loadKubeConfigFromFile(String filePath) {
        Optional<InputStream> inputStreamOptional = resourceResolver.getResourceAsStream(filePath);
        InputStream inputStream = inputStreamOptional.orElseThrow(
            () -> new IllegalArgumentException("Kube config not found: " + filePath));
        Map<String, Object> configMap = loadKubeConfig(inputStream);
        return new KubeConfig(filePath, configMap);
    }

    /**
     * Loads kube config from the given input stream.
     *
     * @param inputStream the input stream
     * @return instance of {@link KubeConfig}
     */
    protected KubeConfig loadKubeConfigFromInputStream(InputStream inputStream) {
        Map<String, Object> configMap = loadKubeConfig(inputStream);
        return new KubeConfig(configMap);
    }

    private Map<String, Object> loadKubeConfig(InputStream inputStream) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        return yaml.load(inputStream);
    }
}
