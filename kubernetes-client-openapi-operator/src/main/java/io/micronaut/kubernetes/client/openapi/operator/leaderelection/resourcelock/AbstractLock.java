package io.micronaut.kubernetes.client.openapi.operator.leaderelection.resourcelock;

import io.micronaut.core.util.StringUtils;
import io.micronaut.json.JsonMapper;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import io.micronaut.kubernetes.client.openapi.operator.configuration.LeaderElectionConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LeaderElectionRecord;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.Lock;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.LockIdentityProvider;
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver;
import io.micronaut.runtime.ApplicationConfiguration;

import java.io.IOException;

abstract class AbstractLock implements Lock {

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
                new IllegalArgumentException("Failed to resolve leader elector resource name. " +
                    "Configure the application name `" + ApplicationConfiguration.APPLICATION_NAME + "` or " +
                    "provide the lock name explicitly `" +
                    LeaderElectionConfiguration.PREFIX + "`."))
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
    public String describe() {
        return namespace + "/" + name;
    }

    LeaderElectionRecord getLeaderElectionRecord(V1ObjectMeta objectMeta) throws IOException {
        String recordString = objectMeta.getAnnotations() == null ? null : objectMeta.getAnnotations().get(LEADER_ANNOTATION_KEY);
        if (StringUtils.isEmpty(recordString)) {
            return new LeaderElectionRecord();
        }
        return jsonMapper.readValue(recordString, LeaderElectionRecord.class);
    }

    void addLeaderElectionRecord(V1ObjectMeta objectMeta, LeaderElectionRecord record) throws IOException {
        String recordString = jsonMapper.writeValueAsString(record);
        objectMeta.putAnnotationsItem(LEADER_ANNOTATION_KEY, recordString);
    }
}
