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
package io.micronaut.kubernetes.client.operator.leaderelection;

import io.kubernetes.client.extended.leaderelection.resourcelock.ConfigMapLock;
import io.kubernetes.client.extended.leaderelection.resourcelock.EndpointsLock;
import io.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock;
import io.kubernetes.client.openapi.ApiClient;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Secondary;
import io.micronaut.kubernetes.client.NamespaceResolver;
import io.micronaut.kubernetes.client.operator.configuration.LeaderElectionConfiguration;
import io.micronaut.kubernetes.client.operator.configuration.LeaderElectionConfigurationProperties;
import io.micronaut.runtime.ApplicationConfiguration;
import jakarta.inject.Singleton;

/**
 * The factory that creates the official Kubernetes SDK provided
 * {@link io.kubernetes.client.extended.leaderelection.Lock} implementations.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Factory
public class ResourceLockFactory {

    private final ApiClient apiClient;

    private final String lockName;
    private final String lockNamespace;
    private final String appIdentity;

    public ResourceLockFactory(LockIdentityProvider lockIdentityProvider,
                               NamespaceResolver namespaceResolver,
                               ApplicationConfiguration applicationConfiguration,
                               LeaderElectionConfiguration leaderElectionConfiguration,
                               ApiClient apiClient) {

        this.lockName = leaderElectionConfiguration.getResourceName().orElseGet(() ->
                applicationConfiguration.getName().orElseThrow(() ->
                        new IllegalArgumentException("Failed to resolve leader elector resource name. " +
                                "Configure the application name `" + ApplicationConfiguration.APPLICATION_NAME + "` or " +
                                "provide the lock name explicitly `" +
                                LeaderElectionConfigurationProperties.PREFIX + "`."))
        );
        this.lockNamespace = leaderElectionConfiguration.getResourceNamespace().orElseGet(namespaceResolver::resolveNamespace);
        this.appIdentity = lockIdentityProvider.getIdentity();
        this.apiClient = apiClient;
    }

    /**
     * Creates the {@link ConfigMapLock}.
     *
     * @return config map lock
     */
    @Requires(property = "kubernetes.client.operator.leader-election.lock.resource-kind", value = "configmap")
    @Primary
    @Singleton
    public ConfigMapLock configMapLock() {
        return new ConfigMapLock(lockNamespace, lockName, appIdentity, apiClient);
    }

    /**
     * Creates the {@link EndpointsLock}.
     *
     * @return the endpoints lock
     */
    @Requires(property = "kubernetes.client.operator.leader-election.lock.resource-kind", value = "endpoints")
    @Primary
    @Singleton
    public EndpointsLock endpointsLock() {
        return new EndpointsLock(lockNamespace, lockName, appIdentity, apiClient);
    }

    /**
     * Creates the {@link LeaseLock}.
     *
     * @return the lease lock
     */
    @Requires(property = "kubernetes.client.operator.leader-election.lock.resource-kind", value = "lease", defaultValue = "lease")
    @Secondary
    @Singleton
    public LeaseLock leaseLock() {
        return new LeaseLock(lockNamespace, lockName, appIdentity, apiClient);
    }
}
