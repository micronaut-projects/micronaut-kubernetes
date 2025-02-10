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
package io.micronaut.kubernetes.client.openapi.informer;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.util.Toggleable;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;

import java.time.Duration;

/**
 * The informer configuration.
 */
@Internal
@ConfigurationProperties(InformerConfiguration.PREFIX)
public interface InformerConfiguration extends Toggleable {
    String PREFIX = KubernetesClientConfiguration.PREFIX + ".informer";

    /**
     * Timout for informer to get synchronised.
     *
     * @return timeout for informer to get synchronised
     */
    @Bindable(defaultValue = "60s")
    Duration getSyncTimeout();

    /**
     * Timeout step to check whether the informer has synchronised.
     *
     * @return step timeout in milliseconds
     */
    @Bindable(defaultValue = "200ms")
    Duration getSyncStepTimeout();
}
