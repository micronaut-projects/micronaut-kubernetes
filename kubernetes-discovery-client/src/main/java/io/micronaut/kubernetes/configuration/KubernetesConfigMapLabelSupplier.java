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
package io.micronaut.kubernetes.configuration;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.kubernetes.KubernetesConfiguration;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import jakarta.inject.Singleton;

/**
 * Based on configuration dynamically evaluates the label selector for config maps.
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
public class KubernetesConfigMapLabelSupplier extends AbstractKubernetesConfigLabelSupplier {

    public KubernetesConfigMapLabelSupplier(CoreV1ApiReactorClient coreV1ApiReactorClient, KubernetesConfiguration configuration) {
        super(coreV1ApiReactorClient, configuration);
    }

    @Override
    KubernetesConfiguration.AbstractConfigConfiguration getConfig() {
        return configuration.getConfigMaps();
    }
}
