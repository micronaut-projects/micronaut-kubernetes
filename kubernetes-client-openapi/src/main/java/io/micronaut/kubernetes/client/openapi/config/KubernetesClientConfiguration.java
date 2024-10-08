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
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.io.file.FileSystemResourceLoader;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.util.Toggleable;
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
@Internal
@BootstrapContextCompatible
@ConfigurationProperties(KubernetesClientConfiguration.PREFIX)
@Requires(property = KubernetesClientConfiguration.PREFIX + ".enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
public class KubernetesClientConfiguration implements Toggleable {

    public static final String PREFIX = "kubernetes.client";

    private final ResourceResolver resourceResolver;

    private String kubeConfigPath;

    private boolean enabled = true;

    private KubeConfig kubeConfig;

    KubernetesClientConfiguration(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    /**
     * Sets kube config path.
     *
     * @param kubeConfigPath kube config path
     */
    void setKubeConfigPath(String kubeConfigPath) {
        this.kubeConfigPath = kubeConfigPath;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables kubernetes client.
     *
     * @param enabled {@code true} to enable kubernetes client
     */
    void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
