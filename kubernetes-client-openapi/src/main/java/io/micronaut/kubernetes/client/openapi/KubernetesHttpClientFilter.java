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

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.config.model.AuthInfo;
import io.micronaut.kubernetes.client.openapi.credential.KubernetesCredentialLoader;

/**
 * Filter which sets the authorization request header with basic or bearer token
 * if the client certificate authentication is not enabled.
 */
@ClientFilter(serviceId = "kubernetes-client")
@Requires(beans = KubernetesClientConfiguration.class)
class KubernetesHttpClientFilter {

    private final KubeConfig kubeConfig;
    private final KubernetesCredentialLoader kubernetesCredentialLoader;

    KubernetesHttpClientFilter(KubernetesClientConfiguration kubernetesClientConfiguration,
                               KubernetesCredentialLoader kubernetesCredentialLoader) {
        kubeConfig = kubernetesClientConfiguration.getKubeConfig();
        this.kubernetesCredentialLoader = kubernetesCredentialLoader;
    }

    @RequestFilter
    void doFilter(MutableHttpRequest<?> request) {
        if (kubeConfig.isClientCertAuthEnabled()) {
            return;
        }
        if (kubeConfig.isBasicAuthEnabled()) {
            AuthInfo user = kubeConfig.getUser();
            request.basicAuth(user.username(), user.password());
            return;
        }
        String token = kubernetesCredentialLoader.getToken();
        if (StringUtils.isNotEmpty(token)) {
            request.bearerAuth(token);
        }
    }
}
