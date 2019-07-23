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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

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
    private KubernetesDiscoveryConfiguration discovery = new KubernetesDiscoveryConfiguration();
    private KubernetesSecretsConfiguration secrets = new KubernetesSecretsConfiguration();
    private KubernetesConfigMapsConfiguration configMaps = new KubernetesConfigMapsConfiguration();

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
        return this.discovery;
    }

    /**
     * @param discoveryConfiguration The discovery configuration
     */
    public void setDiscovery(KubernetesDiscoveryConfiguration discoveryConfiguration) {
        this.discovery = discoveryConfiguration;
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
     * @return the {@link KubernetesSecretsConfiguration}.
     */
    @Nonnull
    public KubernetesSecretsConfiguration getSecrets() {
        return secrets;
    }

    /**
     * @param secretsConfiguration the {@link KubernetesSecretsConfiguration}.
     */
    public void setSecrets(KubernetesSecretsConfiguration secretsConfiguration) {
        this.secrets = secretsConfiguration;
    }

    /**
     * @return The config maps configuration properties
     */
    @Nonnull
    public KubernetesConfigMapsConfiguration getConfigMaps() {
        return configMaps;
    }

    /**
     * @param configMapsConfiguration The config maps configuration properties
     */
    public void setConfigMaps(KubernetesConfigMapsConfiguration configMapsConfiguration) {
        this.configMaps = configMapsConfiguration;
    }

    @Override
    public String toString() {
        return "KubernetesConfiguration{" +
                "namespace='" + namespace + '\'' +
                ", connectionPoolConfiguration=" + connectionPoolConfiguration +
                ", discovery=" + discovery +
                ", secrets=" + secrets +
                ", configMaps=" + configMaps +
                '}';
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

    private static abstract class AbstractKubernetesConfiguration {
        private Collection<String> includes = new HashSet<>();
        private Collection<String> excludes = new HashSet<>();
        private Collection<Map<String, String>> labels;

        /**
         * @return the names to include
         */
        public Collection<String> getIncludes() {
            return includes;
        }

        /**
         * @param includes the names to include
         */
        public void setIncludes(Collection<String> includes) {
            this.includes = includes;
        }

        /**
         * @return the names to exclude
         */
        public Collection<String> getExcludes() {
            return excludes;
        }

        /**
         * @param excludes the names to exclude
         */
        public void setExcludes(Collection<String> excludes) {
            this.excludes = excludes;
        }

        /**
         * @return labels to match
         */
        public Collection<Map<String, String>> getLabels() {
            if (labels == null) {
                return Collections.emptyList();
            }
            return labels;
        }

        /**
         * @param labels labels to match
         */
        public void setLabels(Collection<Map<String, String>> labels) {
            this.labels = labels;
        }
    }

    /**
     * Kubernetes secrets configuration properties.
     */
    @ConfigurationProperties(KubernetesSecretsConfiguration.PREFIX)
    @BootstrapContextCompatible
    public static class KubernetesSecretsConfiguration extends AbstractKubernetesConfiguration implements Toggleable {

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

    /**
     * Kubernetes secrets configuration properties.
     */
    @ConfigurationProperties(KubernetesConfigMapsConfiguration.PREFIX)
    @BootstrapContextCompatible
    public static class KubernetesConfigMapsConfiguration extends AbstractKubernetesConfiguration {

        static final String PREFIX = "config-maps";

    }
}
