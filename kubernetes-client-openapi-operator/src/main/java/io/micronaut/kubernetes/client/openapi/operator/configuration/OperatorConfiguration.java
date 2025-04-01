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
package io.micronaut.kubernetes.client.openapi.operator.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.util.Toggleable;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;

import java.time.Duration;

/**
 * Operator configuration properties.
 */
@Internal
@ConfigurationProperties(OperatorConfiguration.PREFIX)
public interface OperatorConfiguration extends Toggleable {
    String PREFIX = KubernetesClientConfiguration.PREFIX + ".operator";

    String DEFAULT_WORKER_COUNT = "4";
    String DEFAULT_READY_TIMEOUT = "30s";
    String DEFAULT_READY_CHECK_INTERVAL = "1s";

    /**
     * The operator controller worker count. Default {@value #DEFAULT_WORKER_COUNT}.
     *
     * @return controller worker count
     */
    @Bindable(defaultValue = DEFAULT_WORKER_COUNT)
    int getWorkerCount();

    /**
     * Timeout to wait on informers to be synced. Default {@value #DEFAULT_READY_TIMEOUT}.
     *
     * @return ready timeout
     */
    @Bindable(defaultValue = DEFAULT_READY_TIMEOUT)
    Duration getReadyTimeout();

    /**
     * How often the informer sync status should be checked until they are ready or timeout expires.
     * Default {@value #DEFAULT_READY_CHECK_INTERVAL}.
     *
     * @return ready check interval
     */
    @Bindable(defaultValue = DEFAULT_READY_CHECK_INTERVAL)
    Duration getReadyCheckInternal();
}
