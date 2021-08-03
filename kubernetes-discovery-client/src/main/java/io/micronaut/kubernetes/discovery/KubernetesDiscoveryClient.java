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
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.kubernetes.client.v1.*;
import io.micronaut.kubernetes.client.v1.services.ServiceList;
import io.micronaut.kubernetes.discovery.provider.KubernetesServiceInstanceEndpointProvider;
import io.micronaut.kubernetes.util.KubernetesUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.micronaut.kubernetes.client.v1.KubernetesClient.SERVICE_ID;

/**
 * A {@link DiscoveryClient} implementation for Kubernetes using the API.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@Requires(beans = {KubernetesClient.class, KubernetesConfiguration.KubernetesDiscoveryConfiguration.class})
@Requires(property = KubernetesConfiguration.KubernetesDiscoveryConfiguration.PREFIX + ".enabled", notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@SuppressWarnings("WeakerAccess")
public class KubernetesDiscoveryClient implements DiscoveryClient {

    protected static final Logger LOG = LoggerFactory.getLogger(KubernetesDiscoveryClient.class);

    private final KubernetesClient client;
    private final KubernetesConfiguration configuration;
    private final KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration;
    private final Map<String, KubernetesServiceConfiguration> serviceConfigurations;
    private final Map<String, KubernetesServiceInstanceProvider> instanceProviders;
    private final KubernetesServiceInstanceList instanceList;

    /**
     * Creates discovery client that operates with endpoints only, so the discovery modes are not supported.
     *
     * @param client An HTTP Client to query the Kubernetes API.
     * @param configuration The configuration properties
     * @param discoveryConfiguration The discovery configuration properties
     * @param serviceConfigurations The manual service discovery configurations
     * @param instanceList The {@link KubernetesServiceInstanceList}
     * @deprecated
     * This constructor is no longer used as it doesn't support the discovery modes.
     * Use {@link KubernetesDiscoveryClient#KubernetesDiscoveryClient(KubernetesClient, KubernetesConfiguration, KubernetesConfiguration.KubernetesDiscoveryConfiguration, List, List, KubernetesServiceInstanceList)} instead.
     */
    @Deprecated
    public KubernetesDiscoveryClient(KubernetesClient client,
                                     KubernetesConfiguration configuration,
                                     KubernetesConfiguration.KubernetesDiscoveryConfiguration discoveryConfiguration,
                                     List<KubernetesServiceConfiguration> serviceConfigurations,
                                     KubernetesServiceInstanceList instanceList) {
        this(client, configuration, discoveryConfiguration, serviceConfigurations,
                Collections.singletonList(new KubernetesServiceInstanceEndpointProvider(client, discoveryConfiguration)),
                instanceList);
    }

    /**
     * Creates discovery client that supports the discovery modes.
     *
     * @param client An HTTP Client to query the Kubernetes API.
     * @param configuration The configuration properties
     * @param discoveryConfiguration The discovery configuration properties
     * @param serviceConfigurations The manual service discovery configurations
     * @param instanceProviders The service instance provider implementations
     * @param instanceList The {@link KubernetesServiceInstanceList}
     */
    @Inject
    public KubernetesDiscoveryClient(KubernetesClient client,
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
     *
     * @return A list of services metadata's name.
     */
    @Override
    public Publisher<List<String>> getServiceIds() {
        String namespace = configuration.getNamespace();
        Map<String, String> labels = configuration.getDiscovery().getLabels();
        String labelSelector = KubernetesUtils.computeLabelSelector(labels);
        Predicate<KubernetesObject> includesFilter = KubernetesUtils.getIncludesFilter(discoveryConfiguration.getIncludes());
        Predicate<KubernetesObject> excludesFilter = KubernetesUtils.getExcludesFilter(discoveryConfiguration.getExcludes());

        return Flux.merge(
                Flux.fromIterable(serviceConfigurations.keySet()),
                Flux.from(client.listServices(namespace, labelSelector))
                        .doOnError(throwable -> LOG.error("Error while trying to list all Kubernetes Services in the namespace [" + namespace + "]", throwable))
                        .flatMapIterable(ServiceList::getItems)
                        .filter(includesFilter)
                        .filter(excludesFilter)
                        .map(service -> service.getMetadata().getName())
        ).distinct().collectList();
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
