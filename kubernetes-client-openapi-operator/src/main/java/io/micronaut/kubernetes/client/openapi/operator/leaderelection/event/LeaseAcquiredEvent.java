package io.micronaut.kubernetes.client.openapi.operator.leaderelection.event;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LeaderElectionRecord;

/**
 * Event fired when this service instance has become a leader.
 */
public record LeaseAcquiredEvent(@NonNull LeaderElectionRecord leaderElectionRecord) {
}
