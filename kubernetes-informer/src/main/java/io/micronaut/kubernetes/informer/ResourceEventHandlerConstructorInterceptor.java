/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.informer;

import io.kubernetes.client.apimachinery.GroupVersionKind;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.ListerWatcher;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.Namespaces;
import io.kubernetes.client.util.Watchable;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.options.ListOptions;
import io.micronaut.aop.ConstructorInterceptor;
import io.micronaut.aop.ConstructorInvocationContext;
import io.micronaut.aop.InterceptorBean;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.ModelMapper;
import io.micronaut.kubernetes.client.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Constructor interceptor for the {@link ResourceEventHandler} beans annotated by {@link Informer} annotation that
 * based on provided parameters in the {@link Informer} annotation created the {@link SharedIndexInformer} and registers
 * {@link ResourceEventHandler} to the informer.
 *
 * @param <ApiType> type of Kubernetes Object
 * @author Pavol Gressa
 * @since 3.1
 */
@Requires(beans = SharedInformerFactory.class)
@InterceptorBean(Informer.class)
@BootstrapContextCompatible
public class ResourceEventHandlerConstructorInterceptor<ApiType extends KubernetesObject> implements ConstructorInterceptor<ResourceEventHandler<ApiType>> {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceEventHandlerConstructorInterceptor.class);

    private final SharedInformerFactory sharedInformerFactory;
    private final NamespaceResolver namespaceResolver;
    private final ApiClient apiClient;
    private final ApplicationContext applicationContext;

    private final ModelMapper modelMapper = new ModelMapper();

    public ResourceEventHandlerConstructorInterceptor(SharedInformerFactory sharedInformerFactory,
                                                      NamespaceResolver namespaceResolver,
                                                      ApiClient apiClient,
                                                      ApplicationContext applicationContext) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.namespaceResolver = namespaceResolver;
        this.apiClient = apiClient;
        this.applicationContext = applicationContext;
    }

    @Override
    public @NonNull
    ResourceEventHandler<ApiType> intercept(@NonNull ConstructorInvocationContext<ResourceEventHandler<ApiType>> context) {
        final Class<ResourceEventHandler<ApiType>> declaringType = context.getDeclaringType();

        if (declaringType.isAnnotationPresent(Informer.class)) {
            Informer typeAnnotation = declaringType.getAnnotation(Informer.class);

            // resolve label selector
            String labelSelector = null;
            if (!Objects.equals(typeAnnotation.labelSelector(), "")) {
                labelSelector = typeAnnotation.labelSelector();
            }

            if (!Objects.equals(typeAnnotation.labelSelectorSupplier(), EmptyLabelSupplier.class)) {
                Class<? extends Supplier<String>> selectorSupplierClass = typeAnnotation.labelSelectorSupplier();
                Supplier<String> supplierBean = applicationContext.createBean(selectorSupplierClass);
                labelSelector = labelSelector == null ? supplierBean.get() : labelSelector + "," + supplierBean.get();
            }

            // resolve namespace
            String namespace = typeAnnotation.namespace();
            if (namespace == null || namespace.length() == 0) {
                namespace = namespaceResolver.resolveNamespace();
            } else if (namespace.equals(Informer.ALL_NAMESPACES)) {
                namespace = Namespaces.NAMESPACE_ALL;
            }

            SharedIndexInformer<? extends KubernetesObject> informer = createInformer(
                    typeAnnotation.apiType(), typeAnnotation.apiListType(), typeAnnotation.resourcePlural(),
                    namespace, labelSelector, typeAnnotation.resyncCheckPeriod());
            ResourceEventHandler resourceEventHandler = context.proceed();
            informer.addEventHandler(resourceEventHandler);
            return resourceEventHandler;
        } else {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to create informer for the class [{}] that implements ResourceEventHandler. " +
                        "The io.micronaut.kubernetes.informer.@Informer annotation is missing.", declaringType.getName());
            }
        }
        return context.proceed();
    }

    private SharedIndexInformer<ApiType> createInformer(
            Class<? extends KubernetesObject> apiType, Class<? extends KubernetesListObject> apiListType,
            String resourcePlural,
            String namespace,
            String labelSelector,
            long resyncCheckPeriod) {

        final GroupVersionKind groupVersionKind = modelMapper.getGroupVersionKindByClass(apiType);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating Informer for KubernetesObject '{}' with group '{}', version '{}' and namespace '{}'",
                    apiType, groupVersionKind.getGroup(), groupVersionKind.getVersion(), namespace);
        }

        final GenericKubernetesApi kubernetesApi = new GenericKubernetesApi(
                apiType,
                apiListType,
                groupVersionKind.getGroup(),
                groupVersionKind.getVersion(),
                resourcePlural,
                new CustomObjectsApi(apiClient));

        final SharedIndexInformer informer = sharedInformerFactory.sharedIndexInformerFor(
                listerWatcherFor(kubernetesApi, labelSelector, namespace),
                apiType,
                resyncCheckPeriod);

        if (LOG.isInfoEnabled()) {
            LOG.info("Created Informer for '{}' in namespace '{}'", apiType, namespace);
        }
        return informer;
    }

    private <ApiType extends KubernetesObject, ApiListType extends KubernetesListObject>
    ListerWatcher<ApiType, ApiListType> listerWatcherFor(
            GenericKubernetesApi<ApiType, ApiListType> genericKubernetesApi, String labelSelector, String namespace) {

        return new ListerWatcher<ApiType, ApiListType>() {

            public ApiListType list(CallGeneratorParams params) throws ApiException {
                final ExtendedCallGeneratorParams generatorParams = new ExtendedCallGeneratorParams(params.watch, params.resourceVersion, params.timeoutSeconds, labelSelector);
                final ListOptions options = createListOptions(generatorParams);

                if (Namespaces.NAMESPACE_ALL.equals(namespace)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("List all namespaces with params: {}", generatorParams);
                    }
                    return genericKubernetesApi
                            .list(options)
                            .throwsApiException()
                            .getObject();
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("List namespace '{}' with params: {}", namespace, generatorParams);
                    }
                    return genericKubernetesApi
                            .list(namespace, options)
                            .throwsApiException()
                            .getObject();
                }
            }

            public Watchable<ApiType> watch(CallGeneratorParams params) throws ApiException {
                final ExtendedCallGeneratorParams generatorParams = new ExtendedCallGeneratorParams(params.watch, params.resourceVersion, params.timeoutSeconds, labelSelector);
                final ListOptions options = createListOptions(generatorParams);
                if (Namespaces.NAMESPACE_ALL.equals(namespace)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Watch all namespaces with params: {}", generatorParams);
                    }
                    return genericKubernetesApi.watch(options);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Watch namespace '{}' with params: {}", namespace, generatorParams);
                    }
                    return genericKubernetesApi.watch(namespace, options);
                }
            }
        };
    }

    private ListOptions createListOptions(ExtendedCallGeneratorParams params) {
        return new ListOptions() {
            {
                setResourceVersion(params.resourceVersion);
                setTimeoutSeconds(params.timeoutSeconds);
                setLabelSelector(params.labelSelector);
            }
        };
    }

    static class ExtendedCallGeneratorParams extends CallGeneratorParams {
        String labelSelector;

        public ExtendedCallGeneratorParams(Boolean watch, String resourceVersion, Integer timeoutSeconds, String labelSelector) {
            super(watch, resourceVersion, timeoutSeconds);
            this.labelSelector = labelSelector;
        }

        @Override
        public String toString() {
            return "ExtendedCallGeneratorParams{" +
                    "labelSelector='" + labelSelector + '\'' +
                    ", watch=" + watch +
                    ", resourceVersion='" + resourceVersion + '\'' +
                    ", timeoutSeconds=" + timeoutSeconds +
                    '}';
        }
    }
}
