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
package io.micronaut.kubernetes.client.openapi.operator.leaderelection.resourcelock;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.json.JsonMapper;
import io.micronaut.kubernetes.client.openapi.api.CoordinationV1Api;
import io.micronaut.kubernetes.client.openapi.model.V1Lease;
import io.micronaut.kubernetes.client.openapi.model.V1LeaseSpec;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import io.micronaut.kubernetes.client.openapi.operator.configuration.LeaderElectionConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LeaderElectionRecord;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LockIdentityProvider;
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver;
import io.micronaut.runtime.ApplicationConfiguration;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation which uses an instance of {@link V1Lease} to store leader election record.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/leaderelection/resourcelock/LeaseLock.java">LeaseLock</a>
 * </p>
 */
@Singleton
@Requires(property = "kubernetes.client.operator.leader-election.lock.resource-kind", value = "lease", defaultValue = "lease")
final class LeaseLock extends AbstractLock {

    private static final Logger LOG = LoggerFactory.getLogger(LeaseLock.class);

    private final CoordinationV1Api coordinationV1Api;

    private final AtomicReference<V1Lease> leaseRefer = new AtomicReference<>(null);

    LeaseLock(LockIdentityProvider lockIdentityProvider,
              NamespaceResolver namespaceResolver,
              ApplicationConfiguration applicationConfiguration,
              LeaderElectionConfiguration leaderElectionConfiguration,
              JsonMapper jsonMapper,
              CoordinationV1Api coordinationV1Api) {
        super(lockIdentityProvider, namespaceResolver, applicationConfiguration, leaderElectionConfiguration, jsonMapper);
        this.coordinationV1Api = coordinationV1Api;
    }

    @Override
    public LeaderElectionRecord get() {
        V1Lease lease = coordinationV1Api.readNamespacedLease(getName(), getNamespace(), null);
        if (lease == null) {
            return null;
        }
        leaseRefer.set(lease);
        return getRecordFromLease(lease.getSpec());
    }

    @Override
    public boolean create(LeaderElectionRecord leaderElectionRecord) {
        try {
            V1ObjectMeta objectMeta = new V1ObjectMeta();
            objectMeta.setName(getName());
            objectMeta.setNamespace(getNamespace());

            V1Lease lease = new V1Lease();
            lease.setMetadata(objectMeta);
            lease.setSpec(getLeaseSpecFromRecord(leaderElectionRecord));

            V1Lease createdLease = coordinationV1Api.createNamespacedLease(getNamespace(), lease, null, null, null, null);
            leaseRefer.set(createdLease);
            return true;
        } catch (Exception e) {
            if (e instanceof HttpClientResponseException hcre && hcre.getStatus() == HttpStatus.CONFLICT) {
                LOG.debug("Failed to create lease lock", e);
            } else {
                LOG.error("Failed to create lease lock", e);
            }
            return false;
        }
    }

    @Override
    public boolean update(LeaderElectionRecord leaderElectionRecord) {
        try {
            V1Lease latest = leaseRefer.get();
            latest.setSpec(getLeaseSpecFromRecord(leaderElectionRecord));
            V1Lease updatedLease = coordinationV1Api.replaceNamespacedLease(getName(), getNamespace(), latest, null, null, null, null);
            leaseRefer.set(updatedLease);
            return true;
        } catch (Exception e) {
            if (e instanceof HttpClientResponseException hcre && hcre.getStatus() == HttpStatus.CONFLICT) {
                LOG.debug("Failed to update lease lock", e);
            } else {
                LOG.error("Failed to update lease lock", e);
            }
            return false;
        }
    }

    private LeaderElectionRecord getRecordFromLease(V1LeaseSpec lease) {
        return new LeaderElectionRecord(
            lease.getHolderIdentity(),
            lease.getLeaseDurationSeconds(),
            lease.getAcquireTime() == null ? null : new Date(lease.getAcquireTime().toInstant().toEpochMilli()),
            lease.getRenewTime() == null ? null : new Date(lease.getRenewTime().toInstant().toEpochMilli()),
            lease.getLeaseTransitions());
    }

    private V1LeaseSpec getLeaseSpecFromRecord(LeaderElectionRecord record) {
        V1LeaseSpec spec = new V1LeaseSpec();
        spec.setHolderIdentity(record.holderIdentity());
        spec.setLeaseDurationSeconds(record.leaseDurationSeconds());
        spec.setLeaseTransitions(record.leaderTransitions());
        if (record.acquireTime() != null) {
            spec.setAcquireTime(OffsetDateTime.ofInstant(Instant.ofEpochMilli(record.acquireTime().getTime()), ZoneOffset.UTC));
        }
        if (record.renewTime() != null) {
            spec.setRenewTime(OffsetDateTime.ofInstant(Instant.ofEpochMilli(record.renewTime().getTime()), ZoneOffset.UTC));
        }
        return spec;
    }
}
