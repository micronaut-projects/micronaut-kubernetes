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
package io.micronaut.kubernetes.client.openapi;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ProviderUtils;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import io.micronaut.kubernetes.client.openapi.config.KubeConfigLoader;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.config.model.AuthInfo;
import io.micronaut.kubernetes.client.openapi.credential.KubernetesTokenLoader;
import io.micronaut.kubernetes.client.openapi.credential.ReactiveKubernetesTokenLoader;
import io.micronaut.kubernetes.client.openapi.credential.TokenLoader;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

/**
 * Filter which sets the authorization request header with basic or bearer token
 * if the client certificate authentication is not enabled.
 */
@Requires(beans = KubernetesClientConfiguration.class)
@Internal
@BootstrapContextCompatible
@Filter(patterns = Filter.MATCH_ALL_PATTERN, serviceId = KubernetesHttpClientFactory.CLIENT_ID)
final class KubernetesHttpClientFilter implements HttpClientFilter {
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesHttpClientFilter.class);

    private final Provider<KubeConfig> kubeConfigProvider;
    private final Provider<Collection<TokenLoader>> tokenLoaders;
    private final Scheduler scheduler;

    KubernetesHttpClientFilter(Provider<KubeConfigLoader> kubeConfigLoader,
                               ApplicationContext applicationContext,
                               @Named(TaskExecutors.BLOCKING) @Nullable ExecutorService executorService) {
        // Retrieval has to be delegated to filtering, as any of these classes might
        // depend on a client causing a circular dependency.
        this.kubeConfigProvider = ProviderUtils.memoized(
            () -> kubeConfigLoader.get().getKubeConfig());
        this.tokenLoaders = ProviderUtils.memoized(
            () -> applicationContext.getBeansOfType(TokenLoader.class));
        this.scheduler = executorService == null ? null : Schedulers.fromExecutorService(executorService);
    }

    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        KubeConfig kubeConfig = kubeConfigProvider.get();
        if (kubeConfig != null && kubeConfig.getUser() != null) {
            AuthInfo user = kubeConfig.getUser();
            if (user.clientCertificateData() != null && user.clientKeyData() != null) {
                LOG.trace("Using client certificate authentication");
                return chain.proceed(request);
            }
            if (StringUtils.isNotEmpty(user.username()) && StringUtils.isNotEmpty(user.password())) {
                LOG.trace("Using username and password authentication");
                return chain.proceed(request.basicAuth(user.username(), user.password()));
            }
        }
        Collection<TokenLoader> loaders = tokenLoaders.get();
        LOG.trace("Using token authentication, tokenLoaders={}", loaders);
        return Flux.fromIterable(loaders)
            .concatMap(this::getToken)
            .next()
            .switchIfEmpty(Mono.just(StringUtils.EMPTY_STRING))
            .doOnNext(token -> {
                if (StringUtils.isEmpty(token)) {
                    LOG.trace("Token not loaded by any token loader");
                }
            })
            .flatMapMany(token -> StringUtils.isEmpty(token) ? chain.proceed(request) : chain.proceed(request.bearerAuth(token)));
    }

    private Publisher<String> getToken(TokenLoader tokenLoader) {
        if (tokenLoader instanceof ReactiveKubernetesTokenLoader reactiveTokenLoader) {
            return reactiveTokenLoader.getToken();
        } else if (tokenLoader instanceof KubernetesTokenLoader blockingTokenLoader) {
            Mono<String> publisher = Mono.fromCallable(blockingTokenLoader::getToken);
            if (scheduler != null) {
                publisher = publisher.subscribeOn(scheduler);
            }
            return publisher.doOnNext(token -> LOG.trace("Token loaded by {}", blockingTokenLoader.getClass().getName()));
        }
        LOG.error("Found unknown token loader implementation: {}", tokenLoader.getClass().getName());
        return Mono.empty();
    }
}
