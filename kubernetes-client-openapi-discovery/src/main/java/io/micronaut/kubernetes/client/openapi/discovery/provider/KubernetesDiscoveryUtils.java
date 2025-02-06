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
package io.micronaut.kubernetes.client.openapi.discovery.provider;

import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.discovery.KubernetesServiceConfiguration;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import io.micronaut.kubernetes.client.openapi.util.KubernetesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.function.Predicate;

/**
 * Utility methods for Kubernetes Discovery client.
 */
final class KubernetesDiscoveryUtils {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesDiscoveryUtils.class);

    private static final String SECURE_LABEL = "secure";

    /**
     * Creates a filter for kubernetes objects used for creating {@link ServiceInstance}s whose
     * {@link KubernetesServiceConfiguration} is not explicitly set in property files.
     *
     * @param serviceConfiguration   the service configuration
     * @param discoveryConfiguration the discovery configuration
     * @return filter predicate
     */
    static Predicate<KubernetesObject> serviceConfigurationDiscoveryFilter(KubernetesServiceConfiguration serviceConfiguration,
                                                                           KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
        return serviceConfiguration.isManual() ? kubernetesObject -> true : discoveryConfigurationFilter(discoveryConfiguration);
    }

    /**
     * Creates a filter for kubernetes objects used for creating {@link ServiceInstance}s.
     *
     * @param discoveryConfiguration the discovery configuration
     * @return filter predicate
     */
    static Predicate<KubernetesObject> discoveryConfigurationFilter(KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration) {
        Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(discoveryConfiguration.getIncludes());
        Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(discoveryConfiguration.getExcludes());
        Predicate<KubernetesObject> labelsFilter = KubernetesUtils.getLabelsFilter(discoveryConfiguration.getLabels());
        return includesFilter.and(excludesFilter).and(labelsFilter);
    }

    /**
     * Builds a service instance.
     *
     * @param serviceId  the service id
     * @param portName   the port name
     * @param portNumber the port number
     * @param address    the address
     * @param metadata   the metadata
     * @return service instance
     */
    static ServiceInstance buildServiceInstance(String serviceId, String portName, Integer portNumber, String address, V1ObjectMeta metadata) {
        String scheme = isPortSecure(portName, portNumber) || isMetadataSecure(metadata) ? "https://" : "http://";
        URI uri = portNumber == null ? URI.create(scheme + address) : URI.create(scheme + address + ":" + portNumber);
        LOG.trace("Building ServiceInstance for serviceId [{}] and URI [{}] with metadata [{}]", serviceId, uri, metadata);
        return ServiceInstance
            .builder(serviceId, uri)
            .metadata(metadata.getLabels())
            .build();
    }

    /**
     * Attempts to guess whether this port should be connected to using SSL. By default, port numbers
     * ending in {@code 443} or port named {@code https} are considered secure.
     *
     * @param portName   the port name
     * @param portNumber the port number
     * @return Whether the port is considered secure
     */
    private static boolean isPortSecure(String portName, Integer portNumber) {
        return (portNumber != null && String.valueOf(portNumber).endsWith("443")) || "https".equals(portName);
    }

    /**
     * Checks whether there is a label named {@link #SECURE_LABEL} with value set to {@code true}.
     *
     * @param objectMeta the object metadata
     * @return {@code true} if there is a label named {@link #SECURE_LABEL} with value set to {@code true}, {@code false} otherwise
     */
    private static boolean isMetadataSecure(V1ObjectMeta objectMeta) {
        if (objectMeta.getLabels() == null) {
            return false;
        }
        String secure = objectMeta.getLabels().getOrDefault(SECURE_LABEL, "false");
        return StringUtils.TRUE.equals(secure);
    }
}
