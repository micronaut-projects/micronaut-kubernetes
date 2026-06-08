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
package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Based on configuration dynamically evaluates the label selector.
 *
 * @author Pavol Gressa
 * @deprecated Replaced with config import implementation
 */
@Deprecated(forRemoval = true, since = "8.0.0")
abstract sealed class AbstractKubernetesConfigLabelSupplier implements Supplier<String>
    permits KubernetesConfigMapLabelSupplier, KubernetesSecretLabelSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractKubernetesConfigLabelSupplier.class);

    private final CoreV1ApiReactor coreV1ApiReactor;
    private final KubernetesConfiguration configuration;
    private final KubernetesConfiguration.AbstractConfigConfiguration configConfiguration;

    AbstractKubernetesConfigLabelSupplier(CoreV1ApiReactor coreV1ApiReactor,
                                          KubernetesConfiguration configuration,
                                          KubernetesConfiguration.AbstractConfigConfiguration configConfiguration) {
        this.coreV1ApiReactor = coreV1ApiReactor;
        this.configuration = configuration;
        this.configConfiguration = configConfiguration;
    }

    @Nullable
    @Override
    public String get() {
        String labelSelector = KubernetesConfigUtils.computePodLabelSelector(coreV1ApiReactor,
                configConfiguration.getPodLabels(),
                configuration.getNamespace(),
                configConfiguration.getLabels(),
                configuration.getDiscovery().isExceptionOnPodLabelsMissing())
            .block();
        LOG.debug("Computed kubernetes configuration discovery config label selector: [{}]", labelSelector);
        return labelSelector;
    }
}
