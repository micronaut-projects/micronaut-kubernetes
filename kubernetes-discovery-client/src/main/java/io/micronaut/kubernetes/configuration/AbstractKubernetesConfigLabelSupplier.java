/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.kubernetes.configuration;

import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import io.micronaut.kubernetes.util.KubernetesUtils;

/**
 * Based on configuration dynamically evaluates the label selector.
 *
 * @author Pavol Gressa
 */
abstract class AbstractKubernetesConfigLabelSupplier implements Supplier<String> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractKubernetesConfigLabelSupplier.class);

    protected final KubernetesConfiguration configuration;
    private final CoreV1ApiReactorClient coreV1ApiReactorClient;

    AbstractKubernetesConfigLabelSupplier(CoreV1ApiReactorClient coreV1ApiReactorClient, KubernetesConfiguration configuration) {
        this.coreV1ApiReactorClient = coreV1ApiReactorClient;
        this.configuration = configuration;
    }

    @Override
    public String get() {
        Map<String, String> labels = getConfig().getLabels();
        String labelSelector = KubernetesUtils.computePodLabelSelector(coreV1ApiReactorClient,
                getConfig().getPodLabels(), configuration.getNamespace(), labels,
                configuration.getDiscovery().isExceptionOnPodLabelsMissing())
            .block();
        if (LOG.isInfoEnabled()) {
            LOG.info("Computed kubernetes configuration discovery config label selector: {}", labelSelector);
        }
        return labelSelector;
    }

    abstract KubernetesConfiguration.AbstractConfigConfiguration getConfig();
}
