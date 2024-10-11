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
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.io.file.FileSystemResourceLoader;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Default implementation which creates {@link KubeConfig} instance from data stored in a kube config file.
 * It first tries to find the kube config file on the path defined in {@link KubernetesClientConfiguration}
 * and then on the default path {@code HOME_DIR/.kube/conf}.
 */
@Internal
@Singleton
@BootstrapContextCompatible
public final class DefaultKubeConfigLoader extends AbstractKubeConfigLoader {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultKubeConfigLoader.class);

    private final KubernetesClientConfiguration kubernetesClientConfiguration;

    DefaultKubeConfigLoader(ResourceResolver resourceResolver, KubernetesClientConfiguration kubernetesClientConfiguration) {
        super(resourceResolver);
        this.kubernetesClientConfiguration = kubernetesClientConfiguration;
    }

    @Override
    protected KubeConfig loadKubeConfig() {
        String kubeConfigPath = kubernetesClientConfiguration.getKubeConfigPath();
        if (StringUtils.isEmpty(kubeConfigPath)) {
            String homeDir = findHomeDir();
            if (StringUtils.isEmpty(homeDir)) {
                throw new IllegalArgumentException("Kube config file path not provided and home directory not found");
            }
            kubeConfigPath = FileSystemResourceLoader.PREFIX + Path.of(homeDir, ".kube", "config");
        }
        LOG.info("Loading kube config: {}", kubeConfigPath);
        return loadKubeConfigFromFile(kubeConfigPath);
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
