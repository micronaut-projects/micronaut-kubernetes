package io.micronaut.kubernetes.client.openapi.operator.leaderelection;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.Date;

@Serdeable
public record LeaderElectionRecord(
    @Nullable String holderIdentity,
    int leaseDurationSeconds,
    @Nullable Date acquireTime,
    @Nullable Date renewTime,
    int leaderTransitions
) {
    public LeaderElectionRecord() {
        this(null, 0, null, null, 0);
    }
}
