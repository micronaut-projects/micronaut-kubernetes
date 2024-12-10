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
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import io.micronaut.kubernetes.client.openapi.config.KubeConfigLoader;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.config.model.AuthInfo;
import io.micronaut.kubernetes.client.openapi.credential.KubernetesTokenLoader;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Provider;

import java.util.Collection;
import java.util.List;

/**
 * Filter which sets the authorization request header with basic or bearer token
 * if the client certificate authentication is not enabled.
 */
@ClientFilter(serviceId = KubernetesHttpClientFactory.CLIENT_ID)
@Requires(beans = KubernetesClientConfiguration.class)
@Internal
final class KubernetesHttpClientFilter {

    private Provider<KubeConfig> kubeConfigProvider;
    private final Provider<Collection<KubernetesTokenLoader>> kubernetesTokenLoaders;

    KubernetesHttpClientFilter(Provider<KubeConfigLoader> kubeConfigLoader,
                               ApplicationContext applicationContext) {
        // Retrieval has to be delegated to filtering, as any of these classes might
        // depend on a client causing a circular dependency.
        this.kubeConfigProvider = ProviderUtils.memoized(
            () -> kubeConfigLoader.get().getKubeConfig());
        this.kubernetesTokenLoaders = ProviderUtils.memoized(
            () -> applicationContext.getBeansOfType(KubernetesTokenLoader.class));
    }

    @RequestFilter
    @ExecuteOn(TaskExecutors.BLOCKING)
    void doFilter(MutableHttpRequest<?> request) {
        KubeConfig kubeConfig = kubeConfigProvider.get();
        if (kubeConfig != null && kubeConfig.getUser() != null) {
            AuthInfo user = kubeConfig.getUser();
            if (user.clientCertificateData() != null && user.clientKeyData() != null) {
                return;
            }
            if (StringUtils.isNotEmpty(user.username()) && StringUtils.isNotEmpty(user.password())) {
                request.basicAuth(user.username(), user.password());
                return;
            }
        }
        String token = null;
        for (KubernetesTokenLoader kubernetesTokenLoader : kubernetesTokenLoaders.get()) {
            token = kubernetesTokenLoader.getToken();
            if (StringUtils.isNotEmpty(token)) {
                break;
            }
        }
        if (StringUtils.isNotEmpty(token)) {
            request.bearerAuth(token);
        }
    }
}
