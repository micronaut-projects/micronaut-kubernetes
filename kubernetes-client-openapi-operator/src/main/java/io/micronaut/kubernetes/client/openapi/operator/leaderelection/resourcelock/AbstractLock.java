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

import io.micronaut.core.util.StringUtils;
import io.micronaut.json.JsonMapper;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import io.micronaut.kubernetes.client.openapi.operator.configuration.LeaderElectionConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LeaderElectionRecord;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LockIdentityProvider;
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver;
import io.micronaut.runtime.ApplicationConfiguration;

import java.io.IOException;

/**
 * Common methods for lock implementations.
 */
@SuppressWarnings("java:S2637")
abstract sealed class AbstractLock implements Lock permits ConfigMapLock, EndpointsLock, LeaseLock {
    private static final String LEADER_ANNOTATION_KEY = "control-plane.alpha.kubernetes.io/leader";

    private final String namespace;
    private final String name;
    private final String identity;
    private final JsonMapper jsonMapper;

    AbstractLock(LockIdentityProvider lockIdentityProvider,
                 NamespaceResolver namespaceResolver,
                 ApplicationConfiguration applicationConfiguration,
                 LeaderElectionConfiguration leaderElectionConfiguration,
                 JsonMapper jsonMapper) {
        name = leaderElectionConfiguration.getResourceName().orElseGet(() ->
            applicationConfiguration.getName().orElseThrow(() ->
                new IllegalArgumentException("Failed to resolve leader elector resource name. Configure the application name `" +
                    ApplicationConfiguration.APPLICATION_NAME + "` or set the resource name explicitly using `" +
                    LeaderElectionConfiguration.PREFIX + ".resource-name`"))
        );
        namespace = leaderElectionConfiguration.getResourceNamespace().orElseGet(namespaceResolver::resolveNamespace);
        identity = lockIdentityProvider.getIdentity();
        this.jsonMapper = jsonMapper;
    }

    String getNamespace() {
        return namespace;
    }

    String getName() {
        return name;
    }

    @Override
    public String getIdentity() {
        return identity;
    }

    @Override
    public String toString() {
        return namespace + "/" + name;
    }

    LeaderElectionRecord getLeaderElectionRecord(V1ObjectMeta objectMeta) throws IOException {
        String recordString = objectMeta.getAnnotations() == null ? null : objectMeta.getAnnotations().get(LEADER_ANNOTATION_KEY);
        return StringUtils.isEmpty(recordString)
            ? new LeaderElectionRecord()
            : jsonMapper.readValue(recordString, LeaderElectionRecord.class);
    }

    void addLeaderElectionRecord(V1ObjectMeta objectMeta, LeaderElectionRecord leaderElectionRecord) throws IOException {
        String recordString = jsonMapper.writeValueAsString(leaderElectionRecord);
        objectMeta.putAnnotationsItem(LEADER_ANNOTATION_KEY, recordString);
    }
}
