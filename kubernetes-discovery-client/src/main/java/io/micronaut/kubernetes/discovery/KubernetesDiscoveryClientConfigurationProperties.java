/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.kubernetes.discovery;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;

import javax.annotation.Nonnull;

/**
 * {@link io.micronaut.context.annotation.ConfigurationProperties} implementation of {@link KubernetesDiscoveryClientConfiguration}.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
@ConfigurationProperties(KubernetesDiscoveryClientConfigurationProperties.PREFIX)
public class KubernetesDiscoveryClientConfigurationProperties implements KubernetesDiscoveryClientConfiguration {
    public static final String PREFIX = KubernetesConfiguration.PREFIX + ".discovery-client";

    /**
     * The default enable value.
     */
    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_ENABLED = false;

    /**
     * The default namespace value.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String DEFAULT_NAMESPACE = "default";

    private boolean enabled = DEFAULT_ENABLED;

    @Nonnull
    private String namespace = DEFAULT_NAMESPACE;

    /**
     * @return true if you want to enable the {@link KubernetesDiscoveryClient}
     */
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets whether the {@link KubernetesDiscoveryClient} is enabled. Default value ({@value #DEFAULT_ENABLED}).
     *
     * @param enabled True if is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     *
     * @return the namespace
     */
    @Nonnull
    @Override
    public String getNamespace() {
        return namespace;
    }

    /**
     *
     * @param namespace sets the namespace. Default value ({@value #DEFAULT_NAMESPACE}.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
