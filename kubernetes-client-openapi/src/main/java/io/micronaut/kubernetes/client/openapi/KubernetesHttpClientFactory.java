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

import io.micronaut.buffer.netty.NettyByteBufferFactory;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.MediaType;
import io.micronaut.http.bind.DefaultRequestBinderRegistry;
import io.micronaut.http.body.ContextlessMessageBodyHandlerRegistry;
import io.micronaut.http.body.MessageBodyHandlerRegistry;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.LoadBalancer;
import io.micronaut.http.client.filter.ClientFilterResolutionContext;
import io.micronaut.http.client.filter.DefaultHttpClientFilterResolver;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.client.netty.NettyClientCustomizer;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.micronaut.http.netty.body.NettyByteBufMessageBodyHandler;
import io.micronaut.http.netty.body.NettyCharSequenceBodyWriter;
import io.micronaut.http.netty.body.NettyJsonHandler;
import io.micronaut.http.netty.body.NettyJsonStreamHandler;
import io.micronaut.http.netty.body.NettyWritableBodyWriter;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.codec.JsonMediaTypeCodec;
import io.micronaut.json.codec.JsonStreamMediaTypeCodec;
import io.micronaut.kubernetes.client.openapi.ssl.KubernetesClientSslBuilder;
import io.micronaut.kubernetes.client.openapi.ssl.KubernetesPrivateKeyLoader;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.websocket.context.WebSocketBeanRegistry;
import io.netty.channel.Channel;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.net.URI;
import java.util.Collections;

/**
 * Factory for kubernetes http client.
 */
@Factory
@Context
@Internal
@BootstrapContextCompatible
@Requires(beans = KubernetesClientConfiguration.class)
class KubernetesHttpClientFactory {
    static final String CLIENT_ID = "kubernetes-client";

    private final KubeConfig kubeConfig;
    private final KubernetesPrivateKeyLoader kubernetesPrivateKeyLoader;
    private final ResourceResolver resourceResolver;
    private final DefaultHttpClientFilterResolver defaultHttpClientFilterResolver;

    KubernetesHttpClientFactory(KubernetesClientConfiguration kubernetesClientConfiguration,
                                KubernetesPrivateKeyLoader kubernetesPrivateKeyLoader,
                                ResourceResolver resourceResolver,
                                DefaultHttpClientFilterResolver defaultHttpClientFilterResolver) {
        kubeConfig = kubernetesClientConfiguration.getKubeConfig();
        this.kubernetesPrivateKeyLoader = kubernetesPrivateKeyLoader;
        this.resourceResolver = resourceResolver;
        this.defaultHttpClientFilterResolver = defaultHttpClientFilterResolver;
    }

    @Singleton
    @Named(CLIENT_ID)
    @BootstrapContextCompatible
    protected DefaultHttpClient getKubernetesHttpClient() {
        URI uri = URI.create(kubeConfig.getCluster().server());

        return new DefaultHttpClient(LoadBalancer.fixed(uri),
            null,
            new DefaultHttpClientConfiguration(),
            null,
            defaultHttpClientFilterResolver,
            defaultHttpClientFilterResolver.resolveFilterEntries(new ClientFilterResolutionContext(Collections.singletonList("kubernetes-client"), null)),
            new DefaultThreadFactory(MultithreadEventLoopGroup.class),
            new KubernetesClientSslBuilder(resourceResolver, kubeConfig, kubernetesPrivateKeyLoader),
            createDefaultMediaTypeRegistry(),
            createDefaultMessageBodyHandlerRegistry(),
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

    private static MediaTypeCodecRegistry createDefaultMediaTypeRegistry() {
        JsonMapper mapper = JsonMapper.createDefault();
        ApplicationConfiguration configuration = new ApplicationConfiguration();
        return MediaTypeCodecRegistry.of(
            new JsonMediaTypeCodec(mapper, configuration, null),
            new JsonStreamMediaTypeCodec(mapper, configuration, null)
        );
    }

    private static MessageBodyHandlerRegistry createDefaultMessageBodyHandlerRegistry() {
        ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        ContextlessMessageBodyHandlerRegistry registry = new ContextlessMessageBodyHandlerRegistry(
            applicationConfiguration,
            NettyByteBufferFactory.DEFAULT,
            new NettyByteBufMessageBodyHandler(),
            new NettyWritableBodyWriter(applicationConfiguration)
        );
        JsonMapper mapper = JsonMapper.createDefault();
        registry.add(MediaType.APPLICATION_JSON_TYPE, new NettyJsonHandler<>(mapper));
        registry.add(MediaType.APPLICATION_JSON_TYPE, new NettyCharSequenceBodyWriter());
        registry.add(MediaType.APPLICATION_JSON_STREAM_TYPE, new NettyJsonStreamHandler<>(mapper));
        return registry;
    }
}
