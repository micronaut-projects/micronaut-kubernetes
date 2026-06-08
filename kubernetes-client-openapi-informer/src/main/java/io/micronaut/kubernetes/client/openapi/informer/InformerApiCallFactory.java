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

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The factory which creates {@link InformerApiCall} instances using mappings between
 * kubernetes api types and {@link ExecutableMethod} instances which can be used for
 * execution of {@code list} and {@code watch} api calls.
 */
@SuppressWarnings("rawtypes")
@Singleton
final class InformerApiCallFactory {
    private static final Logger LOG = LoggerFactory.getLogger(InformerApiCallFactory.class);

    private final ApplicationContext applicationContext;

    private final ApiReactorExecMethodProcessor apiReactorExecMethodProcessor;

    private final ApiWatcherExecMethodProcessor apiWatcherExecMethodProcessor;

    InformerApiCallFactory(ApplicationContext applicationContext,
                           ApiReactorExecMethodProcessor apiReactorExecMethodProcessor,
                           ApiWatcherExecMethodProcessor apiWatcherExecMethodProcessor) {
        this.applicationContext = applicationContext;
        this.apiReactorExecMethodProcessor = apiReactorExecMethodProcessor;
        this.apiWatcherExecMethodProcessor = apiWatcherExecMethodProcessor;
    }

    /**
     * Creates an {@link InformerApiCall} instance which can handle execution of
     * {@code list} and {@code watch} api calls for given api type.
     *
     * @param apiTypeClass the api type
     * @param namespace    the namespace should be provided only if {@code list} and {@code watch}
     *                     api calls should be restricted to given namespace
     * @param <ApiType>    kubernetes api type
     * @return an instance of {@link InformerApiCall}
    */
    <ApiType extends KubernetesObject> InformerApiCall<ApiType> createInformerApiCall(
        Class<ApiType> apiTypeClass,
        @Nullable String namespace,
        @Nullable String labelSelector) {

        String apiTypeClassName = apiTypeClass.getName();
        boolean useNamespace;
        ExecutableMethod listExecMethod;
        if (StringUtils.isEmpty(namespace)) {
            useNamespace = false;
            listExecMethod = apiReactorExecMethodProcessor.getGlobalExecMethods().get(apiTypeClassName);
        } else {
            ExecutableMethod namespacedListExecMethod = apiReactorExecMethodProcessor.getNamespaceExecMethods().get(apiTypeClassName);
            if (namespacedListExecMethod != null) {
                useNamespace = true;
                listExecMethod = namespacedListExecMethod;
            } else {
                LOG.warn("Usage of namespaced api calls not supported for type '{}', so fallback to global api calls", apiTypeClassName);
                useNamespace = false;
                listExecMethod = apiReactorExecMethodProcessor.getGlobalExecMethods().get(apiTypeClassName);
            }
        }
        if (listExecMethod == null) {
            throw new IllegalArgumentException(apiTypeClassName + " is not supported");
        }
        Class<?> listBeanType = apiReactorExecMethodProcessor.getBeanTypes().get(apiTypeClassName);
        if (listBeanType == null) {
            throw new IllegalArgumentException(apiTypeClassName + " list bean is not supported");
        }
        Object listBean = applicationContext.getBean(listBeanType);

        ExecutableMethod watchExecMethod = useNamespace
            ? apiWatcherExecMethodProcessor.getNamespaceExecMethods().get(apiTypeClassName)
            : apiWatcherExecMethodProcessor.getGlobalExecMethods().get(apiTypeClassName);
        if (watchExecMethod == null) {
            throw new IllegalArgumentException(apiTypeClassName + " watch is not supported");
        }
        Class<?> watchBeanType = apiWatcherExecMethodProcessor.getBeanTypes().get(apiTypeClassName);
        if (watchBeanType == null) {
            throw new IllegalArgumentException(apiTypeClassName + " watch bean is not supported");
        }
        Object watchBean = applicationContext.getBean(watchBeanType);

        return new InformerApiCall<ApiType>(listExecMethod, listBean, watchExecMethod, watchBean, namespace, labelSelector);
    }
}
