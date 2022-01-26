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

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.PodNameResolver;
import jakarta.inject.Singleton;

/**
 * Default implementation of {@link LockIdentityProvider} which resolves the unique lock identity from the {@code HOSTNAME}
 * environment variable.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Singleton
public class DefaultLockIdentityProvider implements LockIdentityProvider {

    private final PodNameResolver podNameResolver;

    public DefaultLockIdentityProvider(@NonNull PodNameResolver podNameResolver) {
        this.podNameResolver = podNameResolver;
    }

    @Override
    public String getIdentity() {
        return podNameResolver.getPodName().orElseThrow(() ->
                new ConfigurationException("Failed to resolve the lock identity from the PodNameResolver. " +
                        "If the application is running outside of the Kubernetes cluster implement custom " +
                "io.micronaut.kubernetes.client.operator.leaderelection.LockIdentityProvider"));
    }
}
