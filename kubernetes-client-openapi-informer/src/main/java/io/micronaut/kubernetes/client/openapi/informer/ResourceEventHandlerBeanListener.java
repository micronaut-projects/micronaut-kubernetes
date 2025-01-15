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

import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;

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

    ResourceEventHandlerBeanListener(SharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ResourceEventHandler<ApiType> onCreated(BeanCreatedEvent<ResourceEventHandler<ApiType>> event) {
        BeanDefinition<ResourceEventHandler<ApiType>> beanDefinition = event.getBeanDefinition();
        if (beanDefinition.hasAnnotation(Informer.class)) {
            AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class);
            Class<? extends KubernetesObject> apiType = annotationValue.classValue("apiType", KubernetesObject.class)
                .orElseThrow(() -> new NullPointerException("The apiType parameter of @Informer is required."));
            String namespace = annotationValue.stringValue("namespace").orElse(null);
            long resyncCheckPeriod = annotationValue.longValue("resyncCheckPeriod").orElse(0L);
            SharedIndexInformer sharedIndexInformer = sharedIndexInformerFactory.sharedIndexInformerFor(apiType, namespace, resyncCheckPeriod);
            sharedIndexInformer.addEventHandler(event.getBean());
        }
        return event.getBean();
    }
}
