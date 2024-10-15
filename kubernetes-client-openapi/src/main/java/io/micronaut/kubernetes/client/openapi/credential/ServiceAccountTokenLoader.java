/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.kubernetes.client.openapi.credential;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration.ServiceAccount;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Loads a token from the service account token file.
 */
@Singleton
@BootstrapContextCompatible
@Internal
@Requires(property = KubernetesClientConfiguration.PREFIX + ".service-account.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
final class ServiceAccountTokenLoader implements KubernetesTokenLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceAccountTokenLoader.class);

    private static final int ORDER = 30;

    private final ResourceResolver resourceResolver;
    private final ServiceAccount serviceAccount;

    private volatile String token;
    private volatile LocalDateTime expirationTime;

    ServiceAccountTokenLoader(ResourceResolver resourceResolver,
                              KubernetesClientConfiguration kubernetesClientConfiguration) {
        this.resourceResolver = resourceResolver;
        serviceAccount = kubernetesClientConfiguration.getServiceAccount();
    }

    @Override
    public String getToken() {
        setToken();
        return token;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private void setToken() {
        if (shouldLoadToken()) {
            synchronized (this) {
                if (shouldLoadToken()) {
                    String tokenPath = serviceAccount.getTokenPath();
                    Duration tokenReloadInterval = serviceAccount.getTokenReloadInterval();
                    try {
                        token = loadToken(tokenPath);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load service account token from file: " + tokenPath, e);
                    }
                    expirationTime = LocalDateTime.now().plusSeconds(tokenReloadInterval.toSeconds());
                }
            }
        }
    }

    private boolean shouldLoadToken() {
        if (token == null || expirationTime == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        LOG.debug("Check whether token reloading needed, now={}, expiration={}", now, expirationTime);
        return expirationTime.isBefore(now);
    }

    private String loadToken(String tokenPath) throws IOException {
        LOG.debug("Loading token from file: {}", tokenPath);
        Optional<InputStream> inputStreamOpt = resourceResolver.getResourceAsStream(tokenPath);
        if (inputStreamOpt.isEmpty()) {
            throw new ConfigurationException("Token file not found: " + tokenPath);
        }
        InputStream inputStream = inputStreamOpt.get();
        return new String(inputStream.readAllBytes());
    }
}
