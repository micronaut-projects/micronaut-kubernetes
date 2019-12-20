/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.kubernetes.health;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.health.HealthStatus;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesClientFilter;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.pods.ContainerStatus;
import io.micronaut.kubernetes.client.v1.pods.Pod;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import io.micronaut.scheduling.TaskExecutors;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static io.micronaut.kubernetes.health.KubernetesHealthIndicator.HOSTNAME_ENV_VARIABLE_IN_PROPERTY_FORMAT;


/**
 * Health indicator to show the current Pod information.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(beans = HealthEndpoint.class)
@Requires(env = Environment.KUBERNETES)
@Requires(property = HOSTNAME_ENV_VARIABLE_IN_PROPERTY_FORMAT)
@Requires(resources = "file:" + KubernetesClientFilter.TOKEN_PATH)
public class KubernetesHealthIndicator extends AbstractHealthIndicator<Map<String, Object>> {

    public static final String NAME = "kubernetes";
    public static final String HOSTNAME_ENV_VARIABLE = "HOSTNAME";
    public static final String HOSTNAME_ENV_VARIABLE_IN_PROPERTY_FORMAT = "hostname";

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesHealthIndicator.class);

    private final KubernetesClient client;
    private final KubernetesConfiguration configuration;
    private Map<String, Object> healthInformation = new LinkedHashMap<>();

    /**
     * Constructor.
     *
     * @param executorService The IO {@link ExecutorService}
     * @param client The Kubernetes client
     * @param configuration The Kubernetes configuration
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public KubernetesHealthIndicator(@Named(TaskExecutors.IO) ExecutorService executorService,
                                     KubernetesClient client,
                                     KubernetesConfiguration configuration) {
        this.executorService = executorService;
        this.client = client;
        this.configuration = configuration;
        this.healthStatus = HealthStatus.UP;

        Single.fromPublisher(this.client.getPod(configuration.getNamespace(), System.getenv(HOSTNAME_ENV_VARIABLE)))
                            .subscribeOn(Schedulers.from(this.executorService))
                            .doOnError(this::processError)
                            .retry(5)
                            .subscribe(this::processPod);

    }

    private void processError(Throwable throwable) {
        LOG.warn("Error while getting Pod information", throwable);
        this.healthInformation.put("error", throwable.getMessage());
        this.healthStatus = HealthStatus.UNKNOWN;
    }

    private void processPod(Pod pod) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing pod: {}", pod);
        }

        this.healthStatus = HealthStatus.UP;

        healthInformation.put("namespace", pod.getMetadata().getNamespace());
        healthInformation.put("podName", pod.getMetadata().getName());
        healthInformation.put("podPhase", pod.getStatus().getPhase());
        healthInformation.put("podIP", pod.getStatus().getPodIP());
        healthInformation.put("hostIP", pod.getStatus().getHostIP());
        healthInformation.put("containerStatuses", pod
                .getStatus()
                .getContainerStatuses()
                .stream()
                .collect(ArrayList::new, KubernetesHealthIndicator::accumulateContainerStatus, ArrayList::addAll));

    }

    private static void accumulateContainerStatus(ArrayList<Object> list, ContainerStatus containerStatus) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing containerStatus: {}", containerStatus);
        }
        Map<String, Object> cs = new LinkedHashMap<>();
        cs.put("name", containerStatus.getName());
        cs.put("image", containerStatus.getImage());
        cs.put("ready", containerStatus.isReady());
        list.add(cs);
    }

    @Override
    protected Map<String, Object> getHealthInformation() {
        return healthInformation;
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
