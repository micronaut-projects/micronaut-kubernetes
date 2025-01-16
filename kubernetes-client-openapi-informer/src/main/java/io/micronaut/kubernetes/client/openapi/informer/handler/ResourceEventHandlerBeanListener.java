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
package io.micronaut.kubernetes.client.openapi.informer.handler;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;

import java.util.ArrayList;
import java.util.Set;

/**
 * BeanCreatedEventListener for the {@link ResourceEventHandler} beans annotated by {@link Informer} annotation that
 * based on provided parameters in the {@link Informer} annotation creates the {@link SharedIndexInformer} and registers
 * {@link ResourceEventHandler} to the informer.
 *
 * @param <ApiType> kubernetes api type
 */
@Context
@Internal
final class ResourceEventHandlerBeanListener<ApiType extends KubernetesObject> implements BeanCreatedEventListener<ResourceEventHandler<ApiType>> {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;
    private final InformerNamespaceResolver informerNamespaceResolver;

    ResourceEventHandlerBeanListener(
        SharedIndexInformerFactory sharedIndexInformerFactory,
        InformerNamespaceResolver informerNamespaceResolver) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.informerNamespaceResolver = informerNamespaceResolver;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ResourceEventHandler<ApiType> onCreated(BeanCreatedEvent<ResourceEventHandler<ApiType>> event) {
        BeanDefinition<ResourceEventHandler<ApiType>> beanDefinition = event.getBeanDefinition();
        ResourceEventHandler eventHandler = event.getBean();
        if (beanDefinition.hasAnnotation(Informer.class)) {
            AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class);
            Class<? extends KubernetesObject> apiType = annotationValue.classValue("apiType", KubernetesObject.class)
                .orElseThrow(() -> new NullPointerException("The apiType parameter of @Informer is required."));
            Set<String> namespaces = informerNamespaceResolver.resolveInformerNamespaces(annotationValue);
            long resyncCheckPeriod = annotationValue.longValue("resyncCheckPeriod").orElse(0L);
            if (CollectionUtils.isEmpty(namespaces)) {
                sharedIndexInformerFactory.sharedIndexInformerFor(apiType, null, resyncCheckPeriod)
                    .addEventHandler(eventHandler);
            } else {
                sharedIndexInformerFactory.sharedIndexInformersFor(apiType, new ArrayList<>(namespaces), resyncCheckPeriod)
                    .forEach(informer -> informer.addEventHandler(eventHandler));
            }
        }
        return eventHandler;
    }
}
