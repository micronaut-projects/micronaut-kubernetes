package io.micronaut.kubernetes.client.openapi.operator.leaderelection;

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.kubernetes.client.openapi.resolver.PodNameResolver;
import jakarta.inject.Singleton;

/**
 * Default implementation of {@link LockIdentityProvider} which resolves the unique lock identity
 * from the {@code HOSTNAME} environment variable.
 */
@Singleton
final class DefaultLockIdentityProvider implements LockIdentityProvider {

    private final PodNameResolver podNameResolver;

    DefaultLockIdentityProvider(PodNameResolver podNameResolver) {
        this.podNameResolver = podNameResolver;
    }

    @Override
    public String getIdentity() {
        return podNameResolver.getPodName().orElseThrow(() ->
            new ConfigurationException("Failed to resolve the lock identity from the PodNameResolver. " +
                "If the application is running outside of the Kubernetes cluster, implement a custom LockIdentityProvider"));
    }
}
