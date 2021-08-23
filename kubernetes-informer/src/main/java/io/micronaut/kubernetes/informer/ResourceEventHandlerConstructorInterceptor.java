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
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.micronaut.aop.ConstructorInterceptor;
import io.micronaut.aop.ConstructorInvocationContext;
import io.micronaut.aop.InterceptorBean;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.ModelMapper;
import io.micronaut.kubernetes.client.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final ModelMapper modelMapper = new ModelMapper();

    public ResourceEventHandlerConstructorInterceptor(SharedInformerFactory sharedInformerFactory, NamespaceResolver namespaceResolver) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.namespaceResolver = namespaceResolver;
    }

    @Override
    public @NonNull
    ResourceEventHandler<ApiType> intercept(@NonNull ConstructorInvocationContext<ResourceEventHandler<ApiType>> context) {
        final Class<ResourceEventHandler<ApiType>> declaringType = context.getDeclaringType();

        if (declaringType.isAnnotationPresent(Informer.class)) {
            Informer typeAnnotation = declaringType.getAnnotation(Informer.class);
            SharedIndexInformer<? extends KubernetesObject> informer = createInformer(
                    typeAnnotation.apiType(), typeAnnotation.apiListType(), typeAnnotation.resourcePlural(),
                    typeAnnotation.namespace(), typeAnnotation.resyncCheckPeriod());
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
            long resyncCheckPeriod) {
        String resourceNamespace = namespace;
        if (resourceNamespace == null || namespace.length() == 0) {
            resourceNamespace = namespaceResolver.resolveNamespace();
        } else {
            if (resourceNamespace.equals(Informer.ALL_NAMESPACES)) {
                resourceNamespace = "";
            }
        }

        final GroupVersionKind groupVersionKind = modelMapper.getGroupVersionKindByClass(apiType);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating Informer for KubernetesObject '{}' with group '{}', version '{}' and namespace '{}'",
                    apiType, groupVersionKind.getGroup(), groupVersionKind.getVersion(), resourceNamespace);
        }

        final GenericKubernetesApi kubernetesApi = new GenericKubernetesApi(
                apiType,
                apiListType,
                groupVersionKind.getGroup(),
                groupVersionKind.getVersion(),
                resourcePlural);
        final SharedIndexInformer informer = sharedInformerFactory.sharedIndexInformerFor(
                kubernetesApi, apiType, resyncCheckPeriod, resourceNamespace);
        if (LOG.isInfoEnabled()) {
            LOG.info("Created Informer for {} in namespace {}", apiType, resourceNamespace);
        }
        return informer;
    }
}
