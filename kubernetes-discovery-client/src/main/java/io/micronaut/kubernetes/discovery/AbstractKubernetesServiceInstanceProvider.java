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

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1EndpointPort;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Abstract implementation of kubernetes service instance provider.
 *
 * @author Pavol Gressa
 * @since 2.3
 */
public abstract class AbstractKubernetesServiceInstanceProvider implements KubernetesServiceInstanceProvider {

    public static final String SECURE_LABEL = "secure";
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractKubernetesServiceInstanceProvider.class);

    /**
     * Builds service instance.
     *
     * @param serviceId   service id
     * @param servicePort servicePort
     * @param address     address
     * @param metadata    metadata
     * @return service instance
     */
    public ServiceInstance buildServiceInstance(String serviceId, @Nullable PortBinder servicePort, String address, V1ObjectMeta metadata) {
        boolean isSecure = (servicePort != null && isPortSecure(servicePort)) || isMetadataSecure(metadata);
        String scheme = isSecure ? "https://" : "http://";
        int portNumber = servicePort != null ? servicePort.getPort() : 80;
        URI uri = URI.create(scheme + address + ":" + portNumber);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Building ServiceInstance for serviceId [{}] and URI [{}] with metadata [{}]", serviceId, uri, metadata);
        }
        return ServiceInstance
                .builder(serviceId, uri)
                .metadata(metadata.getLabels())
                .build();
    }

    /**
     * Attempts to guess whether this port should be connected to using SSL. By default, port numbers ending in 443
     * or port named "https" are considered secure
     *
     * @param servicePort the {@link PortBinder}
     * @return Whether the port is considered secure
     */
    public boolean isPortSecure(PortBinder servicePort) {
        String port = String.valueOf(servicePort.getPort());
        return port.endsWith("443") || "https".equals(servicePort.getName());
    }

    /**
     * @param objectMeta the {@link V1ObjectMeta}
     * @return true if there is a label within {@link V1ObjectMeta#getLabels()} named {@link #SECURE_LABEL} and with value "true";
     * false otherwise
     */
    public boolean isMetadataSecure(V1ObjectMeta objectMeta) {
        if (objectMeta.getLabels() == null) {
            return false;
        }
        String secure = objectMeta.getLabels().getOrDefault(SECURE_LABEL, "false");
        return StringUtils.TRUE.equals(secure);
    }

    /**
     * Validates the necessity of having port configuration based on number of declared {@code ports}.
     *
     * @param ports                list of ports
     * @param serviceConfiguration service configuration
     * @return true if the port configuration is valid otherwise false
     */
    public boolean hasValidPortConfiguration(@Nullable List<PortBinder> ports, KubernetesServiceConfiguration serviceConfiguration) {
        final String name = serviceConfiguration.getName().orElse(null);
        if (name != null && ports != null && ports.size() > 1 && !serviceConfiguration.getPort().isPresent()) {
            LOG.debug("The resource [" + name + "] has multiple ports declared ["
                    + ports.stream().map(PortBinder::getName).collect(Collectors.joining(",")) +
                    "] if you want to to use it in micronaut you have to configure it manually.");

            return false;
        }
        return true;
    }

    /**
     * Creates composite of predicates.
     *
     * @param predicates predicates
     * @return predicate
     */
    @SafeVarargs
    public final Predicate<KubernetesObject> compositePredicate(Predicate<KubernetesObject>... predicates) {
        return s -> {
            for (Predicate<KubernetesObject> p : predicates) {
                boolean test = p.test(s);
                if (!test) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Utility class for transparent access to {@link V1EndpointPort} and {@link V1ServicePort}.
     */
    public static class PortBinder {
        private final String name;
        private final int port;

        public PortBinder(String name, int port) {
            this.name = name;
            this.port = port;
        }

        /**
         * @return port name
         */
        public String getName() {
            return name;
        }

        /**
         * @return port number
         */
        public int getPort() {
            return port;
        }

        public static PortBinder fromServicePort(@Nullable V1ServicePort servicePort) {
            if (servicePort == null) {
                return null;
            }
            return new PortBinder(servicePort.getName(), servicePort.getPort());
        }

        public static PortBinder fromEndpointPort(@Nullable V1EndpointPort endpointPort) {
            if (endpointPort == null) {
                return null;
            }
            return new PortBinder(endpointPort.getName(), endpointPort.getPort());
        }

    }
}
