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
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api;
import io.micronaut.kubernetes.client.openapi.model.V1Endpoints;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import io.micronaut.kubernetes.client.openapi.operator.configuration.LeaderElectionConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LeaderElectionRecord;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LockIdentityProvider;
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver;
import io.micronaut.runtime.ApplicationConfiguration;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation which uses an instance of {@link V1Endpoints} to store leader election record.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/leaderelection/resourcelock/EndpointsLock.java">EndpointsLock</a>
 * </p>
 */
@Singleton
@Requires(property = "kubernetes.client.operator.leader-election.lock.resource-kind", value = "endpoints")
final class EndpointsLock extends AbstractLock {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointsLock.class);

    private final CoreV1Api coreV1Api;

    private final AtomicReference<V1Endpoints> endpointsRefer = new AtomicReference<>(null);

    EndpointsLock(LockIdentityProvider lockIdentityProvider,
                  NamespaceResolver namespaceResolver,
                  ApplicationConfiguration applicationConfiguration,
                  LeaderElectionConfiguration leaderElectionConfiguration,
                  JsonMapper jsonMapper,
                  CoreV1Api coreV1Api) {
        super(lockIdentityProvider, namespaceResolver, applicationConfiguration, leaderElectionConfiguration, jsonMapper);
        this.coreV1Api = coreV1Api;
    }

    @Override
    public LeaderElectionRecord get() throws IOException {
        V1Endpoints endpoints = coreV1Api.readNamespacedEndpoints(getName(), getNamespace(), null);
        if (endpoints == null) {
            return null;
        }
        endpointsRefer.set(endpoints);
        return getLeaderElectionRecord(endpoints.getMetadata());
    }

    @Override
    public boolean create(LeaderElectionRecord leaderElectionRecord) {
        try {
            V1ObjectMeta objectMeta = new V1ObjectMeta();
            objectMeta.setName(getName());
            objectMeta.setNamespace(getNamespace());
            addLeaderElectionRecord(objectMeta, leaderElectionRecord);

            V1Endpoints endpoints = new V1Endpoints();
            endpoints.setMetadata(objectMeta);

            V1Endpoints createdEndpoints = coreV1Api.createNamespacedEndpoints(getNamespace(), endpoints, null, null, null, null);
            endpointsRefer.set(createdEndpoints);
            return true;
        } catch (Exception e) {
            if (e instanceof HttpClientResponseException hcre && hcre.getStatus() == HttpStatus.CONFLICT) {
                LOG.debug("Failed to create endpoints lock", e);
            } else {
                LOG.error("Failed to create endpoints lock", e);
            }
            return false;
        }
    }

    @Override
    public boolean update(LeaderElectionRecord leaderElectionRecord) {
        try {
            V1Endpoints endpoints = endpointsRefer.get();
            addLeaderElectionRecord(endpoints.getMetadata(), leaderElectionRecord);
            V1Endpoints updatedEndpoints = coreV1Api.replaceNamespacedEndpoints(getName(), getNamespace(), endpoints, null, null, null, null);
            endpointsRefer.set(updatedEndpoints);
            return true;
        } catch (Exception e) {
            if (e instanceof HttpClientResponseException hcre && hcre.getStatus() == HttpStatus.CONFLICT) {
                LOG.debug("Failed to update endpoints lock", e);
            } else {
                LOG.error("Failed to update endpoints lock", e);
            }
            return false;
        }
    }
}
