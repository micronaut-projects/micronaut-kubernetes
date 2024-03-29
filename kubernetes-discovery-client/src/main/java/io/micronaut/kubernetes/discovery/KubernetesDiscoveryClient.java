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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A {@link DiscoveryClient} implementation for Kubernetes using the API.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(property = KubernetesConfiguration.KubernetesDiscoveryConfiguration.PREFIX + ".enabled", notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@SuppressWarnings("WeakerAccess")
public class KubernetesDiscoveryClient implements DiscoveryClient {

    public static final String SERVICE_ID = "kubernetes";
    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesDiscoveryClient.class);

    private final CoreV1ApiReactorClient client;
    private final KubernetesConfiguration configuration;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;
    private final Map<String, KubernetesServiceConfiguration> serviceConfigurations;
    private final Map<String, KubernetesServiceInstanceProvider> instanceProviders;
    private final KubernetesServiceInstanceList instanceList;

    /**
     * Creates discovery client that supports the discovery modes.
     *
     * @param client                 An HTTP Client to query the Kubernetes API.
     * @param configuration          The configuration properties
     * @param discoveryConfiguration The discovery configuration properties
     * @param serviceConfigurations  The manual service discovery configurations
     * @param instanceProviders      The service instance provider implementations
     * @param instanceList           The {@link KubernetesServiceInstanceList}
     */
    @Inject
    public KubernetesDiscoveryClient(CoreV1ApiReactorClient client,
                                     KubernetesConfiguration configuration,
                                     KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration,
                                     List<KubernetesServiceConfiguration> serviceConfigurations,
                                     List<KubernetesServiceInstanceProvider> instanceProviders,
                                     KubernetesServiceInstanceList instanceList) {
        this.client = client;
        this.configuration = configuration;
        this.discoveryConfiguration = discoveryConfiguration;
        this.serviceConfigurations = serviceConfigurations.stream()
                .collect(Collectors.toMap(KubernetesServiceConfiguration::getServiceId, Function.identity()));
        this.instanceProviders = instanceProviders.stream()
                .collect(Collectors.toMap(KubernetesServiceInstanceProvider::getMode, Function.identity()));
        this.instanceList = instanceList;
    }

    @Override
    public Publisher<List<ServiceInstance>> getInstances(String serviceId) {
        if (!discoveryConfiguration.isEnabled()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Discovery configuration is not enabled");
            }
            return Publishers.just(Collections.emptyList());
        }
        if (SERVICE_ID.equals(serviceId)) {
            return Publishers.just(instanceList.getInstances());
        } else {
            KubernetesServiceConfiguration serviceConfiguration = serviceConfigurations.computeIfAbsent(
                    serviceId, key -> new KubernetesServiceConfiguration(key, false));

            if (!serviceConfiguration.getNamespace().isPresent()) {
                serviceConfiguration.setNamespace(configuration.getNamespace());
            }

            if (!serviceConfiguration.getName().isPresent()) {
                serviceConfiguration.setName(serviceId);
            }

            if (!serviceConfiguration.getMode().isPresent()) {
                serviceConfiguration.setMode(configuration.getDiscovery().getMode());
            }

            String mode = serviceConfiguration.getMode().get();
            if (!instanceProviders.containsKey(mode)) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Unrecognized kubernetes discovery mode: [" + mode +
                            "], out of supported ones: [ " + String.join(",", instanceProviders.keySet()) + "]");
                }
                return Publishers.just(Collections.emptyList());
            } else {
                return instanceProviders.get(mode).getInstances(serviceConfiguration);
            }
        }
    }

    /**
     * @return A list of services metadata's name.
     */
    @Override
    public Publisher<List<String>> getServiceIds() {
        final String namespace = configuration.getNamespace();
        final KubernetesServiceInstanceProvider instanceProvider = instanceProviders.get(discoveryConfiguration.getMode());

        return Flux.merge(
                        Flux.fromIterable(serviceConfigurations.keySet()),
                        instanceProvider.getServiceIds(namespace)
                )
                .distinct().collectList();
    }

    @Override
    public @NonNull
    String getDescription() {
        return SERVICE_ID;
    }

    @Override
    public void close() {
        //no op
    }
}
