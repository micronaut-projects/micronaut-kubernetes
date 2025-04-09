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
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.util.Toggleable;

import java.time.Duration;
import java.util.Optional;

/**
 * The leader election configuration.
 */
@Internal
@ConfigurationProperties(LeaderElectionConfiguration.PREFIX)
public interface LeaderElectionConfiguration extends Toggleable {
    String PREFIX = OperatorConfiguration.PREFIX + ".leader-election.lock";

    String DEFAULT_LEASE_DURATION = "10s";
    String DEFAULT_RENEW_DEADLINE = "8s";
    String DEFAULT_RETRY_PERIOD = "5s";

    /**
     * The lock lease duration. Default {@value #DEFAULT_LEASE_DURATION}.
     *
     * @return lease duration
     */
    @Bindable(defaultValue = DEFAULT_LEASE_DURATION)
    Duration getLeaseDuration();

    /**
     * The lock renew deadline. If the leader elector fails to renew the lock within
     * the deadline, then the controller loses the lock. Default {@value #DEFAULT_RENEW_DEADLINE}.
     *
     * @return renew deadline
     */
    @Bindable(defaultValue = DEFAULT_RENEW_DEADLINE)
    Duration getRenewDeadline();

    /**
     * The lock acquire retry period. Default {@value #DEFAULT_RETRY_PERIOD}.
     *
     * @return lock acquire retry period.
     */
    @Bindable(defaultValue = DEFAULT_RETRY_PERIOD)
    Duration getRetryPeriod();

    /**
     * The lock resource name. If not provided then the application name is used.
     *
     * @return the lock resource name
     */
    @NonNull
    Optional<String> getResourceName();

    /**
     * The lock resource namespace. If not provided then the application namespace is used.
     *
     * @return the lock resource namespace
     */
    @NonNull
    Optional<String> getResourceNamespace();
}
