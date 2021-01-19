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

import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.v1.*;
import io.reactivex.functions.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract implementation of kubernetes service instance provider.
 *
 * @author Pavol Gressa
 * @since 2.3
 */
public abstract class AbstractKubernetesServiceInstanceProvider implements KubernetesServiceInstanceProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractKubernetesServiceInstanceProvider.class);

    /**
     * Validates the necessity of having port configuration based on numbe of {@code ports}.
     * @param ports list of ports
     * @param serviceConfiguration service configuration
     * @throws IllegalArgumentException if the configuration is invalid e.g. is missing port configuration
     * @return true if the port configuration is valid otherwise false
     */
    public boolean hasValidPortConfiguration(List<Port> ports, KubernetesServiceConfiguration serviceConfiguration) {

        if (ports != null && ports.size() > 1 && !serviceConfiguration.getPort().isPresent()) {
            LOG.error("The resource [" + serviceConfiguration.getName().get() + "] has multiple ports specified [" +
                    ports.stream().map(Port::getName).collect(Collectors.joining(",")) +
                    "] but none is configured.");
            return false;
        }
        return true;
    }

    /**
     * Builds service instance.
     *
     * @param serviceId service id
     * @param port port
     * @param address address
     * @param metadata metadata
     * @return service instance
     */
    public ServiceInstance buildServiceInstance(String serviceId, Port port, InetAddress address, Metadata metadata) {
        boolean isSecure = port.isSecure() || metadata.isSecure();
        String scheme = isSecure ? "https://" : "http://";
        URI uri = URI.create(scheme + address.getHostAddress() + ":" + port.getPort());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Building ServiceInstance for serviceId [{}] and URI [{}]", serviceId, uri.toString());
        }
        return ServiceInstance
                .builder(serviceId, uri)
                .metadata(metadata.getLabels())
                .build();
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
}
