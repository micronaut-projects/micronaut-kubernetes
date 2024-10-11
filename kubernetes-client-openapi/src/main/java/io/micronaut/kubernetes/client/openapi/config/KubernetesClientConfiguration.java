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
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.util.Toggleable;

/**
 * Kubernetes client configuration.
 */
@Internal
@BootstrapContextCompatible
@ConfigurationProperties(KubernetesClientConfiguration.PREFIX)
@Requires(property = KubernetesClientConfiguration.PREFIX + ".enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
public class KubernetesClientConfiguration implements Toggleable {

    public static final String PREFIX = "kubernetes.client";

    private String kubeConfigPath;

    private boolean enabled = true;

    /**
     * Gets kube config path.
     *
     * @return kube config path
     */
    public String getKubeConfigPath() {
        return kubeConfigPath;
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
}
