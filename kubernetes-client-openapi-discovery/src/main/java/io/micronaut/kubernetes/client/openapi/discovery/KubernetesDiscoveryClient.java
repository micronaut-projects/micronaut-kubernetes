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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A {@link DiscoveryClient} implementation for Kubernetes using the API.
 *
 * @author Álvaro Sánchez-Mariscal
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(property = KubernetesConfiguration.KubernetesDiscoveryConfiguration.PREFIX + ".enabled", notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@SuppressWarnings("WeakerAccess")
final class KubernetesDiscoveryClient implements DiscoveryClient {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesDiscoveryClient.class);

    private static final String SERVICE_ID = "kubernetes";

    private final KubernetesConfiguration configuration;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;
    private final Map<String, KubernetesServiceConfiguration> serviceConfigurations;
    private final Map<String, KubernetesServiceInstanceProvider> instanceProviders;

    /**
     * Creates discovery client that supports the discovery modes.
     *
     * @param configuration          The configuration properties
     * @param discoveryConfiguration The discovery configuration properties
     * @param serviceConfigurations  The manual service discovery configurations
     * @param instanceProviders      The service instance provider implementations
     */
    @Inject
    KubernetesDiscoveryClient(KubernetesConfiguration configuration,
                              KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration,
                              List<KubernetesServiceConfiguration> serviceConfigurations,
                              List<KubernetesServiceInstanceProvider> instanceProviders) {
        this.configuration = configuration;
        this.discoveryConfiguration = discoveryConfiguration;
        this.serviceConfigurations = serviceConfigurations.stream()
                .collect(Collectors.toConcurrentMap(KubernetesServiceConfiguration::getServiceId, Function.identity()));
        this.instanceProviders = instanceProviders.stream()
                .collect(Collectors.toMap(KubernetesServiceInstanceProvider::getMode, Function.identity()));
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(String serviceId) {
        if (!discoveryConfiguration.isEnabled()) {
            LOG.debug("Discovery configuration is not enabled");
            return Publishers.just(Collections.emptyList());
        }

        KubernetesServiceConfiguration serviceConfiguration = serviceConfigurations.computeIfAbsent(
            serviceId, key -> new KubernetesServiceConfiguration(key, false));

        if (serviceConfiguration.getNamespace().isEmpty()) {
            serviceConfiguration.setNamespace(configuration.getNamespace());
        }

        if (serviceConfiguration.getName().isEmpty()) {
            serviceConfiguration.setName(serviceId);
        }

        String mode;
        Optional<String> modeOpt = serviceConfiguration.getMode();
        if (modeOpt.isPresent()) {
            mode = modeOpt.get();
        } else {
            mode = configuration.getDiscovery().getMode();
            serviceConfiguration.setMode(mode);
        }

        if (!instanceProviders.containsKey(mode)) {
            LOG.error("Unrecognized kubernetes discovery mode: [{}], out of supported ones: {}", mode, instanceProviders.keySet());
            return Publishers.just(Collections.emptyList());
        } else {
            return instanceProviders.get(mode).getInstances(serviceConfiguration);
        }
    }

    /**
     * @return A list of kubernetes object names.
     */
    @Override
    public Publisher<List<String>> getServiceIds() {
        final String namespace = configuration.getNamespace();
        final KubernetesServiceInstanceProvider instanceProvider = instanceProviders.get(discoveryConfiguration.getMode());
        if (instanceProvider == null) {
            LOG.error("Unrecognized kubernetes discovery mode: [{}], out of supported ones: {}", discoveryConfiguration.getMode(), instanceProviders.keySet());
            return Publishers.just(Collections.emptyList());
        }

        return Flux.merge(
                        Flux.fromIterable(serviceConfigurations.keySet()),
                        instanceProvider.getServiceIds(namespace)
                )
                .distinct().collectList();
    }

    @Override
    public String getDescription() {
        return SERVICE_ID;
    }

    @Override
    public void close() {
        //no op
    }
}
