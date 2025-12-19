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
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration.ServiceAccount;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Loads a token from the service account token file.
 */
@Internal
@Singleton
@BootstrapContextCompatible
@Requires(env = Environment.KUBERNETES)
@Requires(property = KubernetesClientConfiguration.PREFIX + ".service-account.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
final class ServiceAccountTokenLoader implements ReactiveKubernetesTokenLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceAccountTokenLoader.class);

    private static final int ORDER = 30;

    private final ResourceResolver resourceResolver;
    private final ServiceAccount serviceAccount;
    private final Scheduler scheduler;

    private volatile String token;
    private volatile LocalDateTime expirationTime;

    ServiceAccountTokenLoader(ResourceResolver resourceResolver,
                              KubernetesClientConfiguration kubernetesClientConfiguration,
                              @Named(TaskExecutors.BLOCKING) @Nullable ExecutorService executorService) {
        this.resourceResolver = resourceResolver;
        serviceAccount = kubernetesClientConfiguration.getServiceAccount();
        this.scheduler = executorService == null ? null : Schedulers.fromExecutorService(executorService);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Publisher<String> getToken() {
        if (!shouldLoadToken()) {
            return Mono.just(token).doOnNext(it -> LOG.trace("Token loaded"));
        }
        Mono<String> publisher = Mono.fromCallable(this::reloadedToken);
        if (scheduler != null) {
            publisher = publisher.subscribeOn(scheduler);
        }
        return publisher.doOnNext(it -> LOG.trace("Token loaded"));
    }

    private String reloadedToken() {
        if (shouldLoadToken()) {
            synchronized (this) {
                if (shouldLoadToken()) {
                    String tokenPath = serviceAccount.getTokenPath();
                    Duration tokenReloadInterval = serviceAccount.getTokenReloadInterval();
                    try {
                        token = loadToken(tokenPath);
                        expirationTime = LocalDateTime.now().plusSeconds(tokenReloadInterval.toSeconds());
                    } catch (Exception e) {
                        LOG.error("Failed to load token from file: {}", tokenPath, e);
                    }
                }
            }
        }
        return token;
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
