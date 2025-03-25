package io.micronaut.kubernetes.client.openapi.operator.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.bind.annotation.Bindable;

import java.time.Duration;
import java.util.Optional;

/**
 * The leader election configuration.
 */
@Internal
@ConfigurationProperties(LeaderElectionConfiguration.PREFIX)
public interface LeaderElectionConfiguration {
    String PREFIX = OperatorConfiguration.PREFIX + ".leader-election.lock";

    /**
     * The lock lease duration.
     *
     * @return lease duration
     */
    @Bindable(defaultValue = "10s")
    Duration getLeaseDuration();

    /**
     * The lock renew deadline. If the leader elector fails to renew the lock within
     * the deadline, then the controller loses the lock.
     *
     * @return renew deadline
     */
    @Bindable(defaultValue = "8s")
    Duration getRenewDeadline();

    /**
     * The lock acquire retry period.
     *
     * @return lock acquire retry period.
     */
    @Bindable(defaultValue = "5s")
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
