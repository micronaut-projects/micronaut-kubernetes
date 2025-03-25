package io.micronaut.kubernetes.client.openapi.operator.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.util.Toggleable;

import java.time.Duration;
import java.util.Optional;

/**
 * Operator configuration properties.
 *
 * @author Pavol Gressa
 */
@Internal
@ConfigurationProperties(OperatorConfiguration.PREFIX)
public interface OperatorConfiguration extends Toggleable {

    String PREFIX = "kubernetes.client.operator";

    String DEFAULT_WORKER_COUNT = "4";

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

    /**
     * How often the informers should be checked for readiness until they are ready or ready timeout expires.
     *
     * @return ready check interval
     */
    Optional<Duration> getReadyCheckInternal();
}
