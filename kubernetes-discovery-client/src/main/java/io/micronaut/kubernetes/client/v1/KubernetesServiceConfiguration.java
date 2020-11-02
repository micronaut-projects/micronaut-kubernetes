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
package io.micronaut.kubernetes.client.v1;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;

import javax.inject.Inject;
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

    @Inject
    public KubernetesServiceConfiguration(@Parameter String serviceId) {
        this.serviceId = serviceId;
    }

    public KubernetesServiceConfiguration(String serviceId, String name, String namespace) {
        this.serviceId = serviceId;
        this.name = name;
        this.namespace = namespace;
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
     * @param serviceId
     */
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * @return service name
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Set service name.
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Namespace of service. If null then default configured namespace is used.
     *
     * @return namespace
     */
    public Optional<String> getNamespace() {
        return Optional.ofNullable(namespace);
    }

    /**
     * Service namespace.
     *
     * @param namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
