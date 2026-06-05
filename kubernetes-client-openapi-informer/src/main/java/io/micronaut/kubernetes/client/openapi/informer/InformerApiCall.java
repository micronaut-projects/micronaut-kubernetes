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

import io.micronaut.core.type.Argument;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.kubernetes.client.openapi.common.KubernetesListObject;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.watcher.WatchEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Handles execution of {@code list} and {@code watch} api calls for given api type.
 *
 * @param <ApiType> api type which extends {@link KubernetesObject}
 */
final class InformerApiCall<ApiType extends KubernetesObject> {

    private final ExecutableMethod<Object, Mono<KubernetesListObject>> listExecMethod;
    private final Object listBean;
    private final ParamHolder listParamHolder;

    private final ExecutableMethod<Object, Flux<WatchEvent<ApiType>>> watchExecMethod;
    private final Object watchBean;
    private final ParamHolder watchParamHolder;

    @Nullable
    private final String namespace;

    InformerApiCall(@NonNull ExecutableMethod<Object, Mono<KubernetesListObject>> listExecMethod,
                    @NonNull Object listBean,
                    @NonNull ExecutableMethod<Object, Flux<WatchEvent<ApiType>>> watchExecMethod,
                    @NonNull Object watchBean,
                    @Nullable String namespace,
                    @Nullable String labelSelector) {
        this.listExecMethod = listExecMethod;
        this.listBean = listBean;
        listParamHolder = new ParamHolder(listExecMethod.getArguments());
        listParamHolder.setValue("namespace", namespace);
        listParamHolder.setValue("labelSelector", labelSelector);
        listParamHolder.setValue("watch", false);

        this.watchExecMethod = watchExecMethod;
        this.watchBean = watchBean;
        watchParamHolder = new ParamHolder(watchExecMethod.getArguments());
        watchParamHolder.setValue("namespace", namespace);
        watchParamHolder.setValue("labelSelector", labelSelector);
        watchParamHolder.setValue("watch", true);

        this.namespace = namespace;
    }

    Mono<KubernetesListObject> list(String resourceVersion) {
        listParamHolder.setValue("resourceVersion", resourceVersion);
        return Objects.requireNonNull(listExecMethod.invoke(listBean, listParamHolder.values), "List API call returned null");
    }

    Flux<WatchEvent<ApiType>> watch(String resourceVersion, int timeoutSeconds) {
        watchParamHolder.setValue("resourceVersion", resourceVersion);
        watchParamHolder.setValue("timeoutSeconds", timeoutSeconds);
        return Objects.requireNonNull(watchExecMethod.invoke(watchBean, watchParamHolder.values), "Watch API call returned null");
    }

    @Nullable
    String getNamespace() {
        return namespace;
    }

    private static final class ParamHolder {
        private final Map<String, Integer> positions = new HashMap<>();
        private final Object[] values;

        private ParamHolder(Argument<?>[] arguments) {
            values = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                positions.put(arguments[i].getName(), i);
            }
        }

        private void setValue(String paramName, @Nullable Object paramValue) {
            Integer position = positions.get(paramName);
            if (position != null) {
                values[position] = paramValue;
            }
        }
    }
}
