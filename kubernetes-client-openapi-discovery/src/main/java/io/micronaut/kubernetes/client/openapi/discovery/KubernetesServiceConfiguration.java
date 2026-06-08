/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.kubernetes.client.openapi.discovery;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Discovery configuration for Kubernetes service.
 */
@Internal
@EachProperty(KubernetesServiceConfiguration.PREFIX)
@BootstrapContextCompatible
public class KubernetesServiceConfiguration {
    static final String PREFIX = KubernetesConfiguration.KubernetesDiscoveryConfiguration.PREFIX + ".services";

    private final String serviceId;
    @Nullable
    private String name;
    @Nullable
    private String namespace;
    @Nullable
    private String mode;
    @Nullable
    private String port;
    private final boolean manual;

    @Inject
    public KubernetesServiceConfiguration(@Parameter String serviceId) {
        this(serviceId, true);
    }

    public KubernetesServiceConfiguration(String serviceId, boolean manual) {
        this.serviceId = serviceId;
        this.manual = manual;
    }

    /**
     * @return service id
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * @return the service name
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Set service name.
     *
     * @param name the service name
     */
    public void setName(@Nullable String name) {
        this.name = name;
    }

    /**
     * Namespace of service. If null then default configured namespace is used.
     *
     * @return namespace the namespace
     */
    public Optional<String> getNamespace() {
        return Optional.ofNullable(namespace);
    }

    /**
     * Service namespace.
     *
     * @param namespace the namespace
     */
    public void setNamespace(@Nullable String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return service discovery mode
     */
    public Optional<String> getMode() {
        return Optional.ofNullable(mode);
    }

    /**
     * Set service discovery mode.
     *
     * @param mode mode
     */
    public void setMode(@Nullable String mode) {
        this.mode = mode;
    }

    /**
     * Port configuration in case of multi-port resource.
     *
     * @return port number
     */
    public Optional<String> getPort() {
        return Optional.ofNullable(port);
    }

    /**
     * Sets port number. Required in case of multi-port resource.
     *
     * @param port port number
     */
    public void setPort(@Nullable String port) {
        this.port = port;
    }

    /**
     * This field is for inner use to mark manually configured services. All configurations
     * on {@link #PREFIX} are manually configured.
     *
     * @return true if manually configured otherwise false
     */
    public boolean isManual() {
        return manual;
    }

    @Override
    public String toString() {
        return "KubernetesServiceConfiguration{" +
                "serviceId='" + serviceId + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", mode='" + mode + '\'' +
                ", port='" + port + '\'' +
                ", manual=" + manual +
                '}';
    }
}
