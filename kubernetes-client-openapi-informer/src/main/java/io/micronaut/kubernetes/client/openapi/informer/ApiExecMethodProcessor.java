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

import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.kubernetes.client.openapi.reactor.annotation.KubernetesClientApiReactor;
import io.micronaut.kubernetes.client.openapi.watcher.annotation.KubernetesClientApiWatcher;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Base class for processing {@link KubernetesClientApiReactor} and {@link KubernetesClientApiWatcher} executable methods.
 *
 * @param <A> kubernetes api annotation type
 */
@SuppressWarnings("java:S1452")
abstract class ApiExecMethodProcessor<A extends Annotation> implements ExecutableMethodProcessor<A> {

    private final Map<String, Class<?>> beanTypes = new HashMap<>();
    private final Map<String, ExecutableMethod<?, ?>> globalExecMethods = new HashMap<>();
    private final Map<String, ExecutableMethod<?, ?>> namespaceExecMethods = new HashMap<>();

    Map<String, Class<?>> getBeanTypes() {
        return Collections.unmodifiableMap(beanTypes);
    }

    Map<String, ExecutableMethod<?, ?>> getGlobalExecMethods() {
        return Collections.unmodifiableMap(globalExecMethods);
    }

    Map<String, ExecutableMethod<?, ?>> getNamespaceExecMethods() {
        return Collections.unmodifiableMap(namespaceExecMethods);
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        Optional<String> returnTypeNameOpt = getReturnTypeName(beanDefinition, method);
        if (returnTypeNameOpt.isPresent()) {
            String returnTypeName = returnTypeNameOpt.get();
            Map<String, ExecutableMethod<?, ?>> map = hasParameter(method, "namespace")
                ? namespaceExecMethods
                : globalExecMethods;
            if (map.containsKey(returnTypeName)) {
                throw new IllegalStateException("The executable methods map already contains an executable method for given type, type: " +
                    returnTypeName + ", existingMethod: " + map.get(returnTypeName).getName() + ", newMethod: " + method.getName());
            }
            map.put(returnTypeName, method);
            beanTypes.put(returnTypeName, beanDefinition.getBeanType());
        }
    }

    abstract Optional<String> getReturnTypeName(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method);

    boolean hasParameter(ExecutableMethod<?, ?> method, String parameterName) {
        Optional<Argument<?>> namespaceArgOpt = Arrays.stream(method.getArguments())
            .filter(arg -> parameterName.equalsIgnoreCase(arg.getName()))
            .findFirst();
        return namespaceArgOpt.isPresent();
    }
}
