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
package io.micronaut.kubernetes.client.openapi.health;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.StringUtils;
import io.micronaut.health.HealthStatus;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.model.V1ContainerStatus;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import io.micronaut.kubernetes.client.openapi.model.V1Pod;
import io.micronaut.kubernetes.client.openapi.model.V1PodStatus;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Health indicator to show the current Pod information.
 */
@Singleton
@Requires(beans = HealthEndpoint.class)
@Requires(env = Environment.KUBERNETES)
@Requires(property = KubernetesHealthIndicator.HOSTNAME_ENV_VARIABLE_IN_PROPERTY_FORMAT)
@Requires(property = HealthEndpoint.PREFIX + ".kubernetes.enabled", notEquals = StringUtils.FALSE)
final class KubernetesHealthIndicator extends AbstractHealthIndicator<Map<String, Object>> {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesHealthIndicator.class);

    private static final String NAME = "kubernetes";

    static final String HOSTNAME_ENV_VARIABLE_IN_PROPERTY_FORMAT = "hostname";

    private final CoreV1ApiReactor client;
    private final KubernetesConfiguration configuration;

    /**
     * Constructor.
     *
     * @param client        The Kubernetes client
     * @param configuration The Kubernetes configuration
     */
    KubernetesHealthIndicator(CoreV1ApiReactor client,
                              KubernetesConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
        this.healthStatus = HealthStatus.UP;
    }

    private Map<String, Object> processError(Throwable throwable) {
        LOG.warn("Error while getting Pod information", throwable);
        Map<String, Object> healthInformation = new LinkedHashMap<>();
        healthInformation.put("error", throwable.getMessage());
        this.healthStatus = HealthStatus.UNKNOWN;
        return healthInformation;
    }

    private Map<String, Object> processPod(V1Pod pod) {
        LOG.trace("Processing pod: {}", pod);

        this.healthStatus = HealthStatus.UP;

        Map<String, Object> healthInformation = new LinkedHashMap<>();

        Optional<V1ObjectMeta> metaOptional = Optional.ofNullable(pod.getMetadata());
        if (metaOptional.isPresent()) {
            V1ObjectMeta objectMeta = metaOptional.get();
            healthInformation.put("namespace", objectMeta.getNamespace());
            healthInformation.put("podName", objectMeta.getName());
        }

        Optional<V1PodStatus> podStatusOptional = Optional.ofNullable(pod.getStatus());
        if (podStatusOptional.isPresent()) {
            V1PodStatus podStatus = podStatusOptional.get();
            healthInformation.put("podPhase", podStatus.getPhase());
            healthInformation.put("podIP", podStatus.getPodIP());
            healthInformation.put("hostIP", podStatus.getHostIP());
            healthInformation.put("containerStatuses",
                    Objects.requireNonNull(podStatus.getContainerStatuses())
                            .stream()
                            .collect(ArrayList::new, KubernetesHealthIndicator::accumulateContainerStatus, ArrayList::addAll));
        }
        LOG.trace("Pod health information: {}", healthInformation);
        return healthInformation;
    }

    private static void accumulateContainerStatus(List<Object> list, V1ContainerStatus containerStatus) {
        LOG.trace("Processing containerStatus: {}", containerStatus);
        Map<String, Object> cs = new LinkedHashMap<>();
        cs.put("name", containerStatus.getName());
        cs.put("image", containerStatus.getImage());
        cs.put("ready", containerStatus.getReady());
        cs.put("restartCount", containerStatus.getRestartCount());
        list.add(cs);
    }

    @Override
    protected Map<String, Object> getHealthInformation() {
        try {
            final String podName = System.getenv(KubernetesConfiguration.HOSTNAME_ENV_VARIABLE);
            if (StringUtils.isEmpty(podName)) {
                return Collections.emptyMap();
            }
            final String podNamespace = configuration.getNamespace();
            V1Pod pod = client.readNamespacedPod(podName, podNamespace, null)
                .doOnError(throwable -> LOG.error("Failed to read Pod [{}] from namespace [{}]", podName, podNamespace, throwable))
                .block();
            return processPod(pod);
        } catch (Exception e) {
            return processError(e);
        }
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
