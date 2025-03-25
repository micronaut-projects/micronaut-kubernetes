package io.micronaut.kubernetes.client.openapi.operator.leaderelection.event;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LeaderElectionRecord;

/**
 * Event fired when this service instance has lost the leader lease.
 */
public record LeaseLostEvent(@Nullable LeaderElectionRecord leaderElectionRecord) {
}
