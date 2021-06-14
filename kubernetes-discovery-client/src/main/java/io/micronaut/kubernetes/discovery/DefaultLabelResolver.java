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
package io.micronaut.kubernetes.discovery;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.util.KubernetesUtils;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.micronaut.kubernetes.health.KubernetesHealthIndicator.HOSTNAME_ENV_VARIABLE;

/**
 * Default implementation of {@link LabelResolver} that always fetches the labels.
 *
 * @author Pavol Gressa
 * @since 2.5
 */
@Requires(env = Environment.KUBERNETES)
@Singleton
public class DefaultLabelResolver implements LabelResolver {
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesUtils.class);

    private final KubernetesClient kubernetesClient;
    private final KubernetesConfiguration kubernetesConfiguration;

    /**
     * Creates default {@link LabelResolver}.
     *
     * @param kubernetesClient
     * @param kubernetesConfiguration
     */
    public DefaultLabelResolver(KubernetesClient kubernetesClient, KubernetesConfiguration kubernetesConfiguration) {
        this.kubernetesClient = kubernetesClient;
        this.kubernetesConfiguration = kubernetesConfiguration;
    }

    @Override
    public Flowable<Map<String, String>> resolveCurrentPodLabels(List<String> podLabelKeys) {
        if (KubernetesUtils.isRunningInKubernetes()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Not running on k8s, skipping resolution of podLabels");
            }
            return Flowable.just(new HashMap<>());
        }
        final String podName = System.getenv(HOSTNAME_ENV_VARIABLE);
        return resolvePodLabels(podName, podLabelKeys);
    }

    @Override
    public Flowable<Map<String, String>> resolvePodLabels(String podName, List<String> podLabelKeys) {
        return Flowable.fromPublisher(kubernetesClient.getPod(kubernetesConfiguration.getNamespace(), podName))
                .map(pod -> pod.getMetadata().getLabels())
                .doOnError(throwable -> {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to get POD " + podName + " : " + throwable.getMessage(), throwable);
                    }
                })
                .onErrorReturnItem(new HashMap<>())
                .map(podLabels -> {
                    Map<String, String> result = new HashMap<>();
                    for (String key : podLabelKeys) {
                        if (!podLabels.containsKey(key)) {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("Pod metadata does not contain label: {}", key);
                            }
                            continue;
                        }

                        String value = podLabels.get(key);
                        if (value != null) {
                            result.put(key, value);
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Including pod label: {}={}", key, value);
                            }
                        } else {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("Pod metadata does not contain value for label: {}", key);
                            }
                        }
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Computed pod label selectors {}", result);
                    }
                    return result;
                });
    }
}
