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
