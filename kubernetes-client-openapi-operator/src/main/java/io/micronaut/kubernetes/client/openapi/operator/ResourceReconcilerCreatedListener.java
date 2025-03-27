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
package io.micronaut.kubernetes.client.openapi.operator;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer;
import io.micronaut.kubernetes.client.openapi.informer.handler.InformerLabelSelectorResolver;
import io.micronaut.kubernetes.client.openapi.informer.handler.InformerNamespaceResolver;
import io.micronaut.kubernetes.client.openapi.operator.configuration.OperatorConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * {@link BeanCreatedEventListener} for the {@link ResourceReconciler} annotated by {@link Operator}.
 * <p>
 * The listener automatically creates the controller infrastructure based on the {@link Operator} configuration.
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Context
@Requires(beans = KubernetesClientConfiguration.class)
@Requires(property = OperatorConfiguration.PREFIX + ".enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
final class ResourceReconcilerCreatedListener<ApiType extends KubernetesObject> implements BeanCreatedEventListener<ResourceReconciler<ApiType>> {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceReconcilerCreatedListener.class);

    private final ControllerFactory controllerFactory;
    private final SharedIndexInformerFactory sharedIndexInformerFactory;
    private final InformerNamespaceResolver namespaceResolver;
    private final InformerLabelSelectorResolver labelSelectorResolver;
    private final BeanContext beanContext;

    ResourceReconcilerCreatedListener(ControllerFactory controllerFactory,
                                      SharedIndexInformerFactory sharedIndexInformerFactory,
                                      InformerNamespaceResolver namespaceResolver,
                                      InformerLabelSelectorResolver labelSelectorResolver,
                                      BeanContext beanContext) {
        this.controllerFactory = controllerFactory;
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.namespaceResolver = namespaceResolver;
        this.labelSelectorResolver = labelSelectorResolver;
        this.beanContext = beanContext;
    }

    @Override
    public ResourceReconciler<ApiType> onCreated(BeanCreatedEvent<ResourceReconciler<ApiType>> event) {
        ResourceReconciler<ApiType> resourceReconciler = event.getBean();

        BeanDefinition<ResourceReconciler<ApiType>> beanDefinition = event.getBeanDefinition();
        if (!beanDefinition.hasAnnotation(Operator.class)) {
            LOG.warn("Bean [{}] implements ResourceReconciler but the @Operator annotation is missing", resourceReconciler);
            return resourceReconciler;
        }

        LOG.debug("Found @Operator annotation on {}", resourceReconciler);

        AnnotationValue<Operator> operatorAnnotationValue = beanDefinition.getAnnotationMetadata().getAnnotation(Operator.class);

        AnnotationValue<Informer> informerAnnotationValue = operatorAnnotationValue.getAnnotation("informer", Informer.class)
            .orElseThrow(() -> new NullPointerException("The informer parameter of @Operator is required."));
        Class apiType = informerAnnotationValue.classValue("apiType", KubernetesObject.class)
            .orElseThrow(() -> new NullPointerException("The apiType parameter of @Informer is required."));
        long resyncCheckPeriod = informerAnnotationValue.longValue("resyncCheckPeriod").orElse(0L);
        Set<String> namespaces = namespaceResolver.resolveInformerNamespaces(informerAnnotationValue);
        String labelSelector = labelSelectorResolver.resolveInformerLabels(informerAnnotationValue);

        String name = operatorAnnotationValue.stringValue("name").orElseGet(() -> "Operator" + apiType.getSimpleName());
        Predicate onAddFilterPredicate = getOnAddFilterPredicate(operatorAnnotationValue);
        BiPredicate onUpdateFilterPredicate = getOnUpdateFilterPredicate(operatorAnnotationValue);
        BiPredicate onDeleteFilterPredicate = getOnDeleteFilterPredicate(operatorAnnotationValue);

        if (CollectionUtils.isEmpty(namespaces)) {
            LOG.trace("Creating {} informer for controller {}", apiType.getSimpleName(), name);
            sharedIndexInformerFactory.sharedIndexInformerFor(apiType, null, labelSelector, false, resyncCheckPeriod);
        } else {
            namespaces.forEach(namespace -> {
                LOG.trace("Creating {} informer in namespace {} for controller {}", apiType.getSimpleName(), namespace, name);
                sharedIndexInformerFactory.sharedIndexInformerFor(apiType, namespace, labelSelector, false, resyncCheckPeriod);
            });
        }

        LOG.debug("Creating controller for {} operator", name);
        controllerFactory.createController(name, apiType, namespaces, resourceReconciler, null, onAddFilterPredicate, onUpdateFilterPredicate, onDeleteFilterPredicate);

        return resourceReconciler;
    }

    private Predicate getOnAddFilterPredicate(AnnotationValue<Operator> annotationValue) {
        Optional<Class<? extends Predicate>> onAddFilterOpt = annotationValue.classValue("onAddFilter", Predicate.class);
        if (onAddFilterOpt.isPresent()) {
            Class<? extends Predicate> onAddFilter = onAddFilterOpt.get();
            if (!Objects.equals(onAddFilter, OperatorFilter.OnAdd.class)) {
                LOG.trace("Found [{}] filter in @Operator's 'onAddFilter' value", onAddFilter.getName());
                return beanContext.getBean(onAddFilter);
            }
        }
        return null;
    }

    private BiPredicate getOnUpdateFilterPredicate(AnnotationValue<Operator> annotationValue) {
        Optional<Class<? extends BiPredicate>> onUpdateFilterOpt = annotationValue.classValue("onUpdateFilter", BiPredicate.class);
        if (onUpdateFilterOpt.isPresent()) {
            Class<? extends BiPredicate> onUpdateFilter = onUpdateFilterOpt.get();
            if (!Objects.equals(onUpdateFilter, OperatorFilter.OnUpdate.class)) {
                LOG.trace("Found [{}] filter in @Operator's 'onUpdateFilter' value", onUpdateFilter.getName());
                return beanContext.getBean(onUpdateFilter);
            }
        }
        return null;
    }

    private BiPredicate getOnDeleteFilterPredicate(AnnotationValue<Operator> annotationValue) {
        Optional<Class<? extends BiPredicate>> onDeleteFilterOpt = annotationValue.classValue("onDeleteFilter", BiPredicate.class);
        if (onDeleteFilterOpt.isPresent()) {
            Class<? extends BiPredicate> onDeleteFilter = onDeleteFilterOpt.get();
            if (!Objects.equals(onDeleteFilter, OperatorFilter.OnDelete.class)) {
                LOG.trace("Found [{}] filter in @Operator's 'onDeleteFilter' value", onDeleteFilter.getName());
                return beanContext.getBean(onDeleteFilter);
            }
        }
        return null;
    }
}
