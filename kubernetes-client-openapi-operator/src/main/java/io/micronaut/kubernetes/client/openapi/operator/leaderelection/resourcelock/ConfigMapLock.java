package io.micronaut.kubernetes.client.openapi.operator.leaderelection.resourcelock;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.json.JsonMapper;
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
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
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/leaderelection/resourcelock/ConfigMapLock.java">ConfigMapLock</a>
 * </p>
 */
@Singleton
@Requires(property = "kubernetes.client.operator.leader-election.lock.resource-kind", value = "configmap")
final class ConfigMapLock extends AbstractLock {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigMapLock.class);

    private final CoreV1Api coreV1Api;

    private final AtomicReference<V1ConfigMap> configMapRefer = new AtomicReference<>(null);

    ConfigMapLock(LockIdentityProvider lockIdentityProvider,
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
        V1ConfigMap configMap = coreV1Api.readNamespacedConfigMap(getName(), getNamespace(), null);
        if (configMap == null) {
            return null;
        }
        configMapRefer.set(configMap);
        return getLeaderElectionRecord(configMap.getMetadata());
    }

    @Override
    public boolean create(LeaderElectionRecord record) {
        try {
            V1ObjectMeta objectMeta = new V1ObjectMeta();
            objectMeta.setName(getName());
            objectMeta.setNamespace(getNamespace());
            addLeaderElectionRecord(objectMeta, record);

            V1ConfigMap configMap = new V1ConfigMap();
            configMap.setMetadata(objectMeta);

            V1ConfigMap createdConfigMap = coreV1Api.createNamespacedConfigMap(getNamespace(), configMap, null, null, null, null);
            configMapRefer.set(createdConfigMap);
            return true;
        } catch (Exception e) {
            if (e instanceof HttpClientResponseException hcre && hcre.getStatus() == HttpStatus.CONFLICT) {
                LOG.debug("Failed to create configmap lock", e);
            } else {
                LOG.error("Failed to create configmap lock", e);
            }
            return false;
        }
    }

    @Override
    public boolean update(LeaderElectionRecord record) {
        try {
            V1ConfigMap configMap = configMapRefer.get();
            addLeaderElectionRecord(configMap.getMetadata(), record);
            V1ConfigMap updatedConfigMap = coreV1Api.replaceNamespacedConfigMap(getName(), getNamespace(), configMap, null, null, null, null);
            configMapRefer.set(updatedConfigMap);
            return true;
        } catch (Exception e) {
            if (e instanceof HttpClientResponseException hcre && hcre.getStatus() == HttpStatus.CONFLICT) {
                LOG.debug("Failed to update configmap lock", e);
            } else {
                LOG.error("Failed to update configmap lock", e);
            }
            return false;
        }
    }
}
