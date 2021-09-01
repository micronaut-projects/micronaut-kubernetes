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
package io.micronaut.kubernetes.discovery;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.kubernetes.KubernetesConfiguration;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * Discovery configuration for Kubernetes service.
 *
 * @since 2.2
 */
@EachProperty(KubernetesServiceConfiguration.PREFIX)
@BootstrapContextCompatible
public class KubernetesServiceConfiguration {
    public static final String NAME = "services";
    public static final String PREFIX = KubernetesConfiguration.KubernetesDiscoveryConfiguration.PREFIX + "." + NAME;

    private String serviceId;
    private String name;
    private String namespace;
    private String mode;
    private String port;
    private final boolean manual;

    @Inject
    public KubernetesServiceConfiguration(@Parameter String serviceId) {
        this(serviceId, null, null, null, null, true);
    }

    public KubernetesServiceConfiguration(String serviceId, boolean manual) {
        this(serviceId, null, null, null, null, manual);
    }

    public KubernetesServiceConfiguration(String serviceId, String name, String namespace) {
        this(serviceId, name, namespace, null, null, false);
    }

    public KubernetesServiceConfiguration(String serviceId, String name, String namespace, String mode, String port, boolean manual) {
        this.serviceId = serviceId;
        this.name = name;
        this.namespace = namespace;
        this.mode = mode;
        this.port = port;
        this.manual = manual;
    }

    /**
     * @return service id
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Set service id.
     *
     * @param serviceId the service id
     */
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
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
    public void setName(String name) {
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
    public void setNamespace(String namespace) {
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
    public void setMode(String mode) {
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
    public void setPort(String port) {
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
