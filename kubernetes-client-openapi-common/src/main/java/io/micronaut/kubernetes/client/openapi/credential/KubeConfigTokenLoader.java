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
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import io.micronaut.kubernetes.client.openapi.config.KubeConfigLoader;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Loads a token from users settings in the kube config file.
 */
@Singleton
@BootstrapContextCompatible
@Internal
final class KubeConfigTokenLoader implements KubernetesTokenLoader {
    private static final Logger LOG = LoggerFactory.getLogger(KubeConfigTokenLoader.class);

    private static final int ORDER = 20;

    private final String token;

    KubeConfigTokenLoader(KubeConfigLoader kubeConfigLoader) {
        KubeConfig kubeConfig = kubeConfigLoader.getKubeConfig();
        token = kubeConfig == null || kubeConfig.getUser() == null ? null : kubeConfig.getUser().token();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Publisher<String> getToken() {
        return StringUtils.isEmpty(token)
            ? Mono.empty()
            : Mono.just(token).doOnNext(token -> LOG.trace("Token loaded by {}", this.getClass().getName()));
    }
}
