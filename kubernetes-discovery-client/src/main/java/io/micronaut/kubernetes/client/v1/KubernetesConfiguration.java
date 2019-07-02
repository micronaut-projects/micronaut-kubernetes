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
import io.micronaut.discovery.DiscoveryConfiguration;
import io.micronaut.discovery.client.DiscoveryClientConfiguration;
import io.micronaut.discovery.registration.RegistrationConfiguration;
import io.micronaut.kubernetes.discovery.KubernetesDiscoveryConfiguration;

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

    public static final String PREFIX = "kubernetes";

    /**
     * The default namespace value.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String DEFAULT_NAMESPACE = "default";

    @Nonnull
    private String namespace = DEFAULT_NAMESPACE;

    private final KubernetesConnectionPoolConfiguration connectionPoolConfiguration;
    private final KubernetesDiscoveryConfiguration discoveryConfiguration;

    /**
     * Default constructor.
     */
    public KubernetesConfiguration() {
        this.connectionPoolConfiguration = new KubernetesConnectionPoolConfiguration();
        this.discoveryConfiguration = new KubernetesDiscoveryConfiguration();
    }

    @Nonnull
    @Override
    public DiscoveryConfiguration getDiscovery() {
        return this.discoveryConfiguration;
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
     *
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
     * The default connection pool configuration.
     */
    @ConfigurationProperties(ConnectionPoolConfiguration.PREFIX)
    @BootstrapContextCompatible
    public static class KubernetesConnectionPoolConfiguration extends ConnectionPoolConfiguration {
    }
}
