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
import io.micronaut.core.annotation.NonNull;

import java.time.Duration;
import java.util.Optional;

/**
 * {@link ConfigurationProperties} implementation of {@link LeaderElectionConfiguration}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@ConfigurationProperties(LeaderElectionConfigurationProperties.PREFIX)
public class LeaderElectionConfigurationProperties implements LeaderElectionConfiguration {

    public static final String PREFIX = OperatorConfigurationProperties.PREFIX + ".leader-election.lock";

    public static final Integer DEFAULT_LEASE_DURATION_IN_SECONDS = 10;
    public static final Integer DEFAULT_RENEW_DEADLINE_IN_SECONDS = 8;
    public static final Integer DEFAULT_RETRY_PERIOD_IN_SECONDS = 5;

    private Duration leaseDuration = Duration.ofSeconds(DEFAULT_LEASE_DURATION_IN_SECONDS);
    private Duration renewDeadline = Duration.ofSeconds(DEFAULT_RENEW_DEADLINE_IN_SECONDS);
    private Duration retryPeriod = Duration.ofSeconds(DEFAULT_RETRY_PERIOD_IN_SECONDS);
    private String resourceName;
    private String resourceNamespace;


    /**
     * The lock lease duration. See {@link io.kubernetes.client.extended.leaderelection.LeaderElector}.
     * Default {@link #DEFAULT_LEASE_DURATION_IN_SECONDS}.
     *
     * @return lock lease duration
     */
    @Override
    @NonNull
    public Duration getLeaseDuration() {
        return leaseDuration;
    }

    /**
     * Sets the lock lease duration.
     * @param leaseDuration lock lease duration.
     */
    public void setLeaseDuration(@NonNull Duration leaseDuration) {
        this.leaseDuration = leaseDuration;
    }

    /**
     * The lock renew deadline. If the {@link io.kubernetes.client.extended.leaderelection.LeaderElector} fails to
     * renew the lock within the deadline then the controller looses the lock. Default {@link #DEFAULT_RENEW_DEADLINE_IN_SECONDS}.
     *
     * @return renew deadline
     */
    @Override
    @NonNull
    public Duration getRenewDeadline() {
        return renewDeadline;
    }

    /**
     * Sets the lock renew deadline. If the {@link io.kubernetes.client.extended.leaderelection.LeaderElector} fails to
     * renew the lock within the deadline then the controller looses the lock.
     *
     * @param renewDeadline lock renew deadline.
     */
    public void setRenewDeadline(@NonNull Duration renewDeadline) {
        this.renewDeadline = renewDeadline;
    }

    /**
     * The lock acquire retry period. See {@link io.kubernetes.client.extended.leaderelection.LeaderElector}. Default
     * {@link #DEFAULT_RETRY_PERIOD_IN_SECONDS}.
     *
     * @return lock acquire retry period.
     */
    @Override
    @NonNull
    public Duration getRetryPeriod() {
        return retryPeriod;
    }

    /**
     * Sets the lock acquire retry period.
     * @param retryPeriod lock acquire retry period
     */
    public void setRetryPeriod(@NonNull Duration retryPeriod) {
        this.retryPeriod = retryPeriod;
    }

    /**
     * The lock resource name. If not provided then the application name is used.
     *
     * @return the lock resource name
     */
    @Override
    @NonNull
    public Optional<String> getResourceName() {
        return Optional.ofNullable(resourceName);
    }

    /**
     * Sets the lock resource name.
     *
     * @param resourceName lock resource name
     */
    public void setResourceName(@NonNull String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * The lock resource namespace. If not provided then the application namespaces is used.
     *
     * @return the lock resource namespace
     */
    @Override
    @NonNull
    public Optional<String> getResourceNamespace() {
        return Optional.ofNullable(resourceNamespace);
    }

    /**
     * Sets the lock resource namespace.
     *
     * @param resourceNamespace the lock resource namespace
     */
    public void setResourceNamespace(@NonNull String resourceNamespace) {
        this.resourceNamespace = resourceNamespace;
    }
}
