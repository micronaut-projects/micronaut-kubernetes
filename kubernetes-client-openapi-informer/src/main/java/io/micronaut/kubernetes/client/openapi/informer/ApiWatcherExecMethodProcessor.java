/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.kubernetes.client.openapi.informer;

import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.kubernetes.client.openapi.watcher.annotation.KubernetesClientApiWatcher;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;

import java.util.Optional;

/**
 * Creates mappings between kubernetes api types and {@link ExecutableMethod} instances which can be
 * used for execution of {@code watch} api calls.
 */
@SuppressWarnings("java:S1872")
@Singleton
final class ApiWatcherExecMethodProcessor extends ApiExecMethodProcessor<KubernetesClientApiWatcher> {

    @Override
    Optional<String> getReturnTypeName(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        if (!Flux.class.getName().equals(method.getReturnType().getType().getName())) {
            return Optional.empty();
        }
        String returnTypeName = method.getReturnType()
            .getWrappedType()
            .getTypeParameters()[0]
            .getType()
            .getName();
        return Optional.of(returnTypeName);
    }
}
