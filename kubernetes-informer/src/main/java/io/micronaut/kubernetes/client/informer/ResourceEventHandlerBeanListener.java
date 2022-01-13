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
package io.micronaut.kubernetes.client.informer;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.kubernetes.client.informer.resolvers.InformerApiGroupResolver;
import io.micronaut.kubernetes.client.informer.resolvers.InformerLabelSelectorResolver;
import io.micronaut.kubernetes.client.informer.resolvers.InformerNamespaceResolver;
import io.micronaut.kubernetes.client.informer.resolvers.InformerResourcePluralResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * BeanCreatedEventListener for the {@link ResourceEventHandler} beans annotated by {@link Informer} annotation that
 * based on provided parameters in the {@link Informer} annotation created the {@link SharedIndexInformer} and registers
 * {@link ResourceEventHandler} to the informer.
 *
 * @param <ApiType> type of Kubernetes Object
 * @author Pavol Gressa
 * @since 3.3
 */
@Requires(beans = SharedInformerFactory.class)
@Context
@Internal
public class ResourceEventHandlerBeanListener<ApiType extends KubernetesObject> implements BeanCreatedEventListener<ResourceEventHandler<ApiType>> {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceEventHandlerBeanListener.class);

    private final SharedIndexInformerFactory sharedIndexInformerFactory;
    private final InformerApiGroupResolver apiGroupResolver;
    private final InformerResourcePluralResolver resourcePluralResolver;
    private final InformerNamespaceResolver namespaceResolver;
    private final InformerLabelSelectorResolver labelSelectorResolver;

    public ResourceEventHandlerBeanListener(SharedIndexInformerFactory sharedIndexInformerFactory,
                                            InformerApiGroupResolver apiGroupResolver,
                                            InformerResourcePluralResolver resourcePluralResolver,
                                            InformerNamespaceResolver namespaceResolver,
                                            InformerLabelSelectorResolver labelSelectorResolver) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.apiGroupResolver = apiGroupResolver;
        this.resourcePluralResolver = resourcePluralResolver;
        this.namespaceResolver = namespaceResolver;
        this.labelSelectorResolver = labelSelectorResolver;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ResourceEventHandler<ApiType> onCreated(BeanCreatedEvent<ResourceEventHandler<ApiType>> event) {
        BeanDefinition<ResourceEventHandler<ApiType>> beanDefinition = event.getBeanDefinition();
        if (beanDefinition.hasAnnotation(Informer.class)) {
            AnnotationValue<Informer> annotationValue = beanDefinition.getAnnotation(Informer.class);
            if (annotationValue != null) {

                ResourceEventHandler resourceEventHandler = event.getBean();

                if (LOG.isTraceEnabled()) {
                    LOG.trace("Found @Informer annotation on {}", resourceEventHandler);
                }

                final Class<? extends KubernetesObject> apiType = InformerAnnotationUtils.resolveApiType(annotationValue);
                final Class<? extends KubernetesListObject> apiListType = InformerAnnotationUtils.resolveApiListType(annotationValue);
                final String resourcePlural = resourcePluralResolver.resolveInformerResourcePlural(annotationValue);
                final String apiGroup = apiGroupResolver.resolveInformerApiGroup(annotationValue);
                final Set<String> namespaces = namespaceResolver.resolveInformerNamespaces(annotationValue);
                final String labelSelector = labelSelectorResolver.resolveInformerLabels(annotationValue);
                final Long resyncCheckPeriod = annotationValue.get("resyncCheckPeriod", Long.class).orElse(0L);

                List<SharedIndexInformer<? extends KubernetesObject>> informers = sharedIndexInformerFactory.sharedIndexInformersFor(
                        apiType,
                        apiListType,
                        resourcePlural,
                        apiGroup,
                        new ArrayList<>(namespaces),
                        labelSelector,
                        resyncCheckPeriod,
                        true);

                informers.forEach(i -> i.addEventHandler(resourceEventHandler));
            } else {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to create informer for the class [{}] that implements ResourceEventHandler. " +
                            "The io.micronaut.kubernetes.informer.@Informer annotation is missing.", "vajco");
                }
            }
        }
        return event.getBean();
    }
}
