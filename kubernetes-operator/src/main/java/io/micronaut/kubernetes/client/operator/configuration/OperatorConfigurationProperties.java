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
package io.micronaut.kubernetes.client.operator.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.util.Toggleable;

import java.time.Duration;
import java.util.Optional;

/**
 * Operator module configuration properties.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@ConfigurationProperties(OperatorConfigurationProperties.PREFIX)
public interface OperatorConfigurationProperties extends Toggleable {

    String PREFIX = "kubernetes.client.operator";

    String DEFAULT_WORKER_COUNT = "16";

    /**
     * The operator controller worker count. Default {@value #DEFAULT_WORKER_COUNT}.
     *
     * @return controller worker count
     */
    @Bindable(defaultValue = DEFAULT_WORKER_COUNT)
    int getWorkerCount();

    /**
     * Timeout to wait before the informers are checked for readiness.
     *
     * @return ready timeout
     */
    Optional<Duration> getReadyTimeout();
}
