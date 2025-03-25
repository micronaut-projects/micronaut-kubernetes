package io.micronaut.kubernetes.client.openapi.operator.leaderelection;

import io.micronaut.context.annotation.DefaultImplementation;
import io.micronaut.core.annotation.NonNull;

/**
 * Provides the lock identity which is used to uniquely identifies the application for the leader
 * election process. Every replica of the application needs to have unique identity.
 */
@DefaultImplementation(DefaultLockIdentityProvider.class)
public interface LockIdentityProvider {

    /**
     * Get the lock identity.
     *
     * @return the lock identity.
     */
    @NonNull String getIdentity();
}
