package io.micronaut.kubernetes.client.openapi.operator.leaderelection.event;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LeaderElectionRecord;

/**
 * Event fired when a leader has changed.
 */
public record LeaderChangedEvent(@NonNull LeaderElectionRecord leaderElectionRecord) {
}
