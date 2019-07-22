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

package io.micronaut.kubernetes.client.v1;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.Toggleable;
import io.micronaut.discovery.DiscoveryConfiguration;
import io.micronaut.discovery.client.DiscoveryClientConfiguration;
import io.micronaut.discovery.registration.RegistrationConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Encapsulates constants for Kubernetes configuration.
 *
 * @author Sergio del Amo
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Requires(env = Environment.KUBERNETES)
@ConfigurationProperties(KubernetesConfiguration.PREFIX)
@BootstrapContextCompatible
public class KubernetesConfiguration extends DiscoveryClientConfiguration {

    public static final String PREFIX = "kubernetes.client";

    /**
     * The default namespace value.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String DEFAULT_NAMESPACE = "default";

    private static final String KUBERNETES_DEFAULT_HOST = "kubernetes";
    private static final int KUBERNETES_DEFAULT_PORT = 443;
    private static final boolean KUBERNETES_DEFAULT_SECURE = true;

    @Nonnull
    private String namespace = DEFAULT_NAMESPACE;

    private KubernetesConnectionPoolConfiguration connectionPoolConfiguration = new KubernetesConnectionPoolConfiguration();
    private KubernetesDiscoveryConfiguration discoveryConfiguration = new KubernetesDiscoveryConfiguration();
    private KubernetesSecretsConfiguration secretsConfiguration = new KubernetesSecretsConfiguration();

    /**
     * Default constructor.
     */
    public KubernetesConfiguration() {
        setPort(KUBERNETES_DEFAULT_PORT);
        setHost(KUBERNETES_DEFAULT_HOST);
        setSecure(KUBERNETES_DEFAULT_SECURE);
    }

    @Nonnull
    @Override
    public DiscoveryConfiguration getDiscovery() {
        return this.discoveryConfiguration;
    }

    /**
     * @param discoveryConfiguration The discovery configuration
     */
    public void setDiscovery(KubernetesDiscoveryConfiguration discoveryConfiguration) {
        this.discoveryConfiguration = discoveryConfiguration;
    }

    @Nullable
    @Override
    public RegistrationConfiguration getRegistration() {
        return null;
    }

    @Override
    protected String getServiceID() {
        return KubernetesClient.SERVICE_ID;
    }

    /**
     * @return the namespace
     */
    @Nonnull
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace Sets the namespace. Default value: {@value #DEFAULT_NAMESPACE}.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        return this.connectionPoolConfiguration;
    }

    /**
     * @param connectionPoolConfiguration the connection pool configuration
     */
    public void setConnectionPoolConfiguration(KubernetesConnectionPoolConfiguration connectionPoolConfiguration) {
        this.connectionPoolConfiguration = connectionPoolConfiguration;
    }

    /**
     * @return the {@link KubernetesSecretsConfiguration}.
     */
    @Nonnull
    public KubernetesSecretsConfiguration getSecrets() {
        return secretsConfiguration;
    }

    /**
     * @param secretsConfiguration the {@link KubernetesSecretsConfiguration}.
     */
    public void setSecrets(KubernetesSecretsConfiguration secretsConfiguration) {
        this.secretsConfiguration = secretsConfiguration;
    }

    /**
     * Configuration class for the discovery client of Kubernetes.
     */
    @ConfigurationProperties(DiscoveryConfiguration.PREFIX)
    @BootstrapContextCompatible
    public static class KubernetesDiscoveryConfiguration extends DiscoveryConfiguration {

        public static final String PREFIX = KubernetesConfiguration.PREFIX + "." + DiscoveryConfiguration.PREFIX;

    }

    /**
     * The default connection pool configuration.
     */
    @ConfigurationProperties(ConnectionPoolConfiguration.PREFIX)
    @BootstrapContextCompatible
    public static class KubernetesConnectionPoolConfiguration extends ConnectionPoolConfiguration {
    }

    /**
     * Kubernetes secrets configuration properties.
     */
    @ConfigurationProperties(KubernetesSecretsConfiguration.PREFIX)
    @BootstrapContextCompatible
    public static class KubernetesSecretsConfiguration implements Toggleable {

        static final String PREFIX = "secrets";

        static final boolean DEFAULT_ENABLED = false;

        private boolean enabled = DEFAULT_ENABLED;

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * @param enabled enabled flag.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
