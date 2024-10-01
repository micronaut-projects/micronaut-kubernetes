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

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Factory
@Context
@Internal
class KubernetesHttpClientFactory {

    static final String CLIENT_ID = "kubernetes-client";

    private final KubernetesClientConfiguration kubernetesClientConfiguration;
    private final KubernetesPrivateKeyLoader kubernetesPrivateKeyLoader;
    private final ResourceResolver resourceResolver;
    private final KubeConfig kubeConfig;

    KubernetesHttpClientFactory(KubernetesClientConfiguration kubernetesClientConfiguration,
                                KubernetesPrivateKeyLoader kubernetesPrivateKeyLoader,
                                ResourceResolver resourceResolver) {
        this.kubernetesClientConfiguration = kubernetesClientConfiguration;
        this.kubernetesPrivateKeyLoader = kubernetesPrivateKeyLoader;
        this.resourceResolver = resourceResolver;
        kubeConfig = loadKubeConfig();
    }

    private KubeConfig loadKubeConfig() {
        Optional<String> kubeConfigPathOptional = kubernetesClientConfiguration.getKubeConfigPath();

        String kubeConfigPath;
        if (kubeConfigPathOptional.isEmpty()) {
            //TODO: use home directory
            kubeConfigPath = "";
        } else {
            kubeConfigPath = kubeConfigPathOptional.get();
        }

        Optional<InputStream> inputStreamOptional = resourceResolver.getResourceAsStream(kubeConfigPath);
        InputStream inputStream = inputStreamOptional.orElseThrow(
            () -> new IllegalArgumentException("Kube config not found: " + kubeConfigPath));
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> configMap = yaml.load(inputStream);
        return new KubeConfig(kubeConfigPath, configMap);
    }

    @Singleton
    @Named(CLIENT_ID)
    @BootstrapContextCompatible
    protected DefaultHttpClient getKubernetesHttpClient() {
        URI uri = URI.create(kubeConfig.getCurrentCluster().server());
        return new DefaultHttpClient(
            uri,
            new DefaultHttpClientConfiguration(),
            new KubernetesClientSslBuilder(resourceResolver, kubeConfig, kubernetesPrivateKeyLoader));
    }
}
