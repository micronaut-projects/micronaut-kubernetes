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

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.io.file.FileSystemResourceLoader;
import io.micronaut.core.util.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Kubernetes client configuration.
 */
@ConfigurationProperties(KubernetesClientConfiguration.PREFIX)
@BootstrapContextCompatible
@Internal
public class KubernetesClientConfiguration {

    public static final String PREFIX = "kubernetes.client";

    private final ResourceResolver resourceResolver;

    private String kubeConfigPath;

    private KubeConfig kubeConfig;

    KubernetesClientConfiguration(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    /**
     * Gets {@link KubeConfig} instance created from {@link #kubeConfigPath} if provided,
     * otherwise created from {HOME_DIR}/.kube/conf file.
     *
     * @return {@link KubeConfig} instance
     */
    public KubeConfig getKubeConfig() {
        if (kubeConfig == null) {
            loadKubeConfig();
        }
        return kubeConfig;
    }

    private void loadKubeConfig() {
        if (StringUtils.isEmpty(kubeConfigPath)) {
            String homeDir = findHomeDir();
            if (StringUtils.isNotEmpty(homeDir)) {
                kubeConfigPath = FileSystemResourceLoader.PREFIX + Path.of(homeDir, ".kube", "config");
            }
        }
        Optional<InputStream> inputStreamOptional = resourceResolver.getResourceAsStream(kubeConfigPath);
        InputStream inputStream = inputStreamOptional.orElseThrow(
            () -> new IllegalArgumentException("Kube config not found: " + kubeConfigPath));
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> configMap = yaml.load(inputStream);
        kubeConfig = new KubeConfig(kubeConfigPath, configMap);
    }

    private String findHomeDir() {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            String userProfile = System.getenv("USERPROFILE");
            if (StringUtils.isNotEmpty(userProfile)) {
                return userProfile;
            }
        }
        String envHome = System.getenv("HOME");
        return StringUtils.isEmpty(envHome) ? null : envHome;
    }
}
