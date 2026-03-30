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
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.bind.DefaultRequestBinderRegistry;
import io.micronaut.http.body.MessageBodyHandlerRegistry;
import io.micronaut.http.client.LoadBalancer;
import io.micronaut.http.client.filter.ClientFilterResolutionContext;
import io.micronaut.http.client.filter.DefaultHttpClientFilterResolver;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.client.netty.NettyClientCustomizer;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import io.micronaut.kubernetes.client.openapi.config.KubeConfigLoader;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.ssl.KubernetesClientSslBuilder;
import io.micronaut.kubernetes.client.openapi.ssl.KubernetesPrivateKeyLoader;
import io.micronaut.websocket.context.WebSocketBeanRegistry;
import io.netty.channel.Channel;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

/**
 * Factory for kubernetes http client.
 */
@Factory
@Context
@Internal
@BootstrapContextCompatible
@Requires(beans = KubernetesClientConfiguration.class)
final class KubernetesHttpClientFactory {

    static final String CLIENT_ID = "kubernetes";
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesHttpClientFactory.class);
    private static final String ENV_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
    private static final String ENV_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";

    private final KubeConfigLoader kubeConfigLoader;
    private final KubernetesClientConfiguration kubernetesClientConfiguration;
    private final KubernetesPrivateKeyLoader kubernetesPrivateKeyLoader;
    private final ResourceResolver resourceResolver;
    private final KubernetesHttpClientConfiguration kubernetesHttpClientConfiguration;
    private final DefaultHttpClientFilterResolver defaultHttpClientFilterResolver;
    private final MessageBodyHandlerRegistry messageBodyHandlerRegistry;

    KubernetesHttpClientFactory(KubeConfigLoader kubeConfigLoader,
                                KubernetesClientConfiguration kubernetesClientConfiguration,
                                KubernetesHttpClientConfiguration kubernetesHttpClientConfiguration,
                                KubernetesPrivateKeyLoader kubernetesPrivateKeyLoader,
                                ResourceResolver resourceResolver,
                                DefaultHttpClientFilterResolver defaultHttpClientFilterResolver,
                                MessageBodyHandlerRegistry messageBodyHandlerRegistry) {
        this.kubeConfigLoader = kubeConfigLoader;
        this.kubernetesClientConfiguration = kubernetesClientConfiguration;
        this.kubernetesPrivateKeyLoader = kubernetesPrivateKeyLoader;
        this.resourceResolver = resourceResolver;
        this.defaultHttpClientFilterResolver = defaultHttpClientFilterResolver;
        this.messageBodyHandlerRegistry = messageBodyHandlerRegistry;
        this.kubernetesHttpClientConfiguration = kubernetesHttpClientConfiguration;
    }

    @Singleton
    @Named(CLIENT_ID)
    @BootstrapContextCompatible
    DefaultHttpClient getKubernetesHttpClient() throws URISyntaxException {
        KubeConfig kubeConfig = kubeConfigLoader.getKubeConfig();
        URI uri;
        if (kubeConfig != null) {
            LOG.debug("Trying to configure client from kube config");
            uri = URI.create(kubeConfig.getCluster().server());
        } else if (kubernetesClientConfiguration.getServiceAccount().isEnabled()) {
            LOG.debug("Trying to configure client from service account");
            String host = System.getenv(ENV_SERVICE_HOST);
            if (StringUtils.isEmpty(host)) {
                throw new ConfigurationException(ENV_SERVICE_HOST + " environment variable not found");
            }
            String port = System.getenv(ENV_SERVICE_PORT);
            if (StringUtils.isEmpty(port)) {
                throw new ConfigurationException(ENV_SERVICE_PORT + " environment variable not found");
            }
            uri = new URI("https", null, host, Integer.parseInt(port), null, null, null);
        } else {
            throw new ConfigurationException("Kube config not provided nor service account authentication enabled");
        }
        Optional<Duration> readTimeout = kubernetesHttpClientConfiguration.getReadTimeout();
        if (readTimeout.isPresent()) {
            kubernetesHttpClientConfiguration.setRequestTimeout(readTimeout.get().plusSeconds(1));
            kubernetesHttpClientConfiguration.setReadTimeout(Duration.ZERO);
        }
        return new DefaultHttpClient(LoadBalancer.fixed(uri),
            null,
            kubernetesHttpClientConfiguration,
            null,
            defaultHttpClientFilterResolver,
            defaultHttpClientFilterResolver.resolveFilterEntries(new ClientFilterResolutionContext(Collections.singletonList(CLIENT_ID), null)),
            new DefaultThreadFactory(MultithreadEventLoopGroup.class),
            new KubernetesClientSslBuilder(resourceResolver, kubeConfig, kubernetesPrivateKeyLoader, kubernetesClientConfiguration),
            null,
            messageBodyHandlerRegistry,
            WebSocketBeanRegistry.EMPTY,
            new DefaultRequestBinderRegistry(ConversionService.SHARED),
            null,
            NioSocketChannel::new,
            NioDatagramChannel::new,
            new NettyClientCustomizer() {
                @Override
                public @NonNull NettyClientCustomizer specializeForChannel(@NonNull Channel channel, @NonNull ChannelRole role) {
                    return NettyClientCustomizer.super.specializeForChannel(channel, role);
                }
            },
            null,
            ConversionService.SHARED,
            null);
    }
}
