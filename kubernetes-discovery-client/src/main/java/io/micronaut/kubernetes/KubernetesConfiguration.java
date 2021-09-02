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
package io.micronaut.kubernetes;


import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.discovery.DiscoveryConfiguration;
import io.micronaut.kubernetes.client.NamespaceResolver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
public class KubernetesConfiguration {

    public static final String PREFIX = "kubernetes.client";

    private String namespace;

    private KubernetesDiscoveryConfiguration discovery = new KubernetesDiscoveryConfiguration();
    private KubernetesSecretsConfiguration secrets = new KubernetesSecretsConfiguration();
    private KubernetesConfigMapsConfiguration configMaps = new KubernetesConfigMapsConfiguration();

    /**
     * Default constructor.
     *
     * @param namespaceResolver namespace resolver
     */
    public KubernetesConfiguration(NamespaceResolver namespaceResolver) {
        this.namespace = namespaceResolver.resolveNamespace();
    }

    /**
     * @return The {@link DiscoveryConfiguration}.
     */
    @NonNull
    public KubernetesDiscoveryConfiguration getDiscovery() {
        return this.discovery;
    }

    /**
     * @param discoveryConfiguration The discovery configuration
     */
    public void setDiscovery(KubernetesDiscoveryConfiguration discoveryConfiguration) {
        this.discovery = discoveryConfiguration;
    }

    /**
     * @return the namespace
     */
    @NonNull
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace Sets the namespace.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return the {@link KubernetesSecretsConfiguration}.
     */
    @NonNull
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
    @NonNull
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
    public static class KubernetesDiscoveryConfiguration extends AbstractKubernetesConfiguration {

        public static final String DEFAULT_MODE = "endpoint";
        public static final String PREFIX = KubernetesConfiguration.PREFIX + "." + DiscoveryConfiguration.PREFIX;

        private String mode = DEFAULT_MODE;

        /**
         * @return default service discovery mode
         */
        public String getMode() {
            return mode;
        }

        /**
         * @param mode default service discovery mode
         */
        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    /**
     * Base class for other configuration sub-classes.
     */
    private abstract static class AbstractKubernetesConfiguration extends DiscoveryConfiguration {
        private Collection<String> includes = new HashSet<>();
        private Collection<String> excludes = new HashSet<>();
        private Map<String, String> labels;
        private List<String> podLabels;

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
        public Map<String, String> getLabels() {
            if (labels == null) {
                return Collections.emptyMap();
            }
            return labels;
        }

        /**
         * @param labels labels to match
         */
        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }

        /**
         * @return podLabels to match
         */
        public List<String> getPodLabels() {
            if (podLabels == null) {
                return Collections.emptyList();
            }
            return podLabels;
        }

        /**
         * @param podLabels labels to match
         */
        public void setPodLabels(List<String> podLabels) {
            this.podLabels = podLabels;
        }
    }

    /**
     * Kubernetes secrets configuration properties.
     */
    @ConfigurationProperties(KubernetesSecretsConfiguration.PREFIX)
    @BootstrapContextCompatible
    public static class KubernetesSecretsConfiguration extends AbstractKubernetesConfiguration {

        static final String PREFIX = "secrets";

        static final boolean DEFAULT_ENABLED = false;

        private boolean enabled = DEFAULT_ENABLED;
        private Collection<String> paths;
        private boolean useApi;

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

        /**
         * @return paths where secrets are mounted
         */
        public Collection<String> getPaths() {
            if (paths == null) {
                return Collections.emptySet();
            }
            return paths;
        }

        /**
         * @param paths where secrets are mounted
         */
        public void setPaths(Collection<String> paths) {
            this.paths = paths;
        }

        /**
         * @return whether to use the API to read secrets when {@link #paths} is used.
         */
        public boolean isUseApi() {
            return useApi;
        }

        /**
         * @param useApi whether to use the API to read secrets when {@link #paths} is used.
         */
        public void setUseApi(boolean useApi) {
            this.useApi = useApi;
        }
    }

    /**
     * Kubernetes config maps configuration properties.
     */
    @ConfigurationProperties(KubernetesConfigMapsConfiguration.PREFIX)
    @BootstrapContextCompatible
    public static class KubernetesConfigMapsConfiguration extends AbstractKubernetesConfiguration {
        public static final String PREFIX = "config-maps";
    }
}
