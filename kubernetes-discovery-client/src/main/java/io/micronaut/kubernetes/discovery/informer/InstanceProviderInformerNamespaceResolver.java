/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.discovery.informer;

import io.micronaut.core.annotation.Internal;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.discovery.KubernetesServiceConfiguration;
import io.micronaut.kubernetes.discovery.KubernetesServiceInstanceProvider;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolver for the namespaces to watch by {@link KubernetesServiceInstanceProvider}.
 *
 * @author Pavol Gressa
 * @since 3.2
 */
@Internal
@Singleton
public class InstanceProviderInformerNamespaceResolver {

    private final KubernetesConfiguration kubernetesConfiguration;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;
    private final List<KubernetesServiceConfiguration> serviceConfigurations;

    public InstanceProviderInformerNamespaceResolver(
            KubernetesConfiguration kubernetesConfiguration,
            KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration,
            List<KubernetesServiceConfiguration> serviceConfigurations) {
        this.kubernetesConfiguration = kubernetesConfiguration;
        this.discoveryConfiguration = discoveryConfiguration;
        this.serviceConfigurations = serviceConfigurations;
    }

    /**
     * Resolves the namespaces to watch.
     *
     * @param kubernetesServiceInstanceProvider service instance provider
     * @return set of namespaces
     */
    public Set<String> resolveInformerNamespaces(KubernetesServiceInstanceProvider kubernetesServiceInstanceProvider) {
        final String mode = kubernetesServiceInstanceProvider.getMode();

        // Resolve namespaces for manually configured service discovery
        Set<String> namespaces = serviceConfigurations.stream()
                .filter(s -> s.getMode().map(ns -> ns.equalsIgnoreCase(mode)).orElse(mode.equalsIgnoreCase(discoveryConfiguration.getMode())))
                .filter(s -> s.getNamespace().isPresent())
                .map(s -> s.getNamespace().get())
                .collect(Collectors.toSet());

        // Add application namespace if the mode equals
        if (discoveryConfiguration.getMode().equalsIgnoreCase(mode)) {
            namespaces.add(kubernetesConfiguration.getNamespace());
        }

        return namespaces;
    }
}
