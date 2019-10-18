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
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.DiscoveryConfiguration;
import io.micronaut.http.client.HttpClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class KubernetesConfiguration extends HttpClientConfiguration {

    public static final String PREFIX = "kubernetes.client";
    public static final String NAMESPACE_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";

    /**
     * The default namespace value.
     */
    public static final String DEFAULT_NAMESPACE = "default";

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfiguration.class);

    private static final String KUBERNETES_DEFAULT_HOST = "kubernetes.default.svc.cluster.local";
    private static final int KUBERNETES_DEFAULT_PORT = 443;
    private static final boolean KUBERNETES_DEFAULT_SECURE = true;

    private String host = KUBERNETES_DEFAULT_HOST;
    private int port = KUBERNETES_DEFAULT_PORT;
    private boolean secure = KUBERNETES_DEFAULT_SECURE;
    private String namespace;

    private KubernetesConnectionPoolConfiguration connectionPoolConfiguration = new KubernetesConnectionPoolConfiguration();
    private KubernetesDiscoveryConfiguration discovery = new KubernetesDiscoveryConfiguration();
    private KubernetesSecretsConfiguration secrets = new KubernetesSecretsConfiguration();
    private KubernetesConfigMapsConfiguration configMaps = new KubernetesConfigMapsConfiguration();

    /**
     * Default constructor.
     */
    public KubernetesConfiguration() {
        if (namespace == null) {
            String namespace = DEFAULT_NAMESPACE;
            try {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Namespace has not been set. Reading it from file [{}]", NAMESPACE_PATH);
                }
                namespace = new String(Files.readAllBytes(Paths.get(NAMESPACE_PATH)));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Namespace: [{}]", namespace);
                }
            } catch (IOException ioe) {
                LOG.warn("An error has occurred when reading the file: [" + NAMESPACE_PATH + "]. Kubernetes namespace will be set to: " + DEFAULT_NAMESPACE, ioe);
            }
            this.namespace = namespace;
        }
    }

    /**
     * @return The Kubernetes API host name
     **/
    @Nonnull
    public String getHost() {
        return host;
    }

    /**
     * @param host The Kubernetes API host name
     */
    public void setHost(String host) {
        if (StringUtils.isNotEmpty(host)) {
            this.host = host;
        }
    }

    /**
     * @return The Kubernetes API port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port The port for the Kubernetes API
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return Is the Kubernetes API server exposed over HTTPS (defaults to true)
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * @param secure Set if the Kubernetes API server is exposed over HTTPS
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * @return The {@link DiscoveryConfiguration}.
     */
    @Nonnull
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
    @Nonnull
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace Sets the namespace.
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
    public static class KubernetesDiscoveryConfiguration extends AbstractKubernetesConfiguration {

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
     * Base class for other configuration sub-classes.
     */
    private abstract static class AbstractKubernetesConfiguration extends DiscoveryConfiguration {
        private Collection<String> includes = new HashSet<>();
        private Collection<String> excludes = new HashSet<>();
        private Map<String, String> labels;

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

        static final String PREFIX = "config-maps";

    }
}
