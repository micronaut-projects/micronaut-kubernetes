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
package io.micronaut.kubernetes.client.operator;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link BeanCreatedEventListener} for the {@link ResourceReconciler} annotated by {@link Operator}.
 * <p>
 * The interceptor automatically creates the controller infrastructure based on the {@link Operator} configuration. This
 * consists from {@link io.kubernetes.client.extended.controller.DefaultController} managed by
 * {@link io.kubernetes.client.extended.controller.ControllerManager} and operated by {@link io.kubernetes.client.extended.controller.LeaderElectingController}.
 *
 * @param <ApiType> the Kubernetes resource api type
 * @author Pavol Gressa
 * @since 3.3
 */
@Requires(beans = SharedInformerFactory.class)
@Context
@Internal
public class ResourceReconcilerCreatedListener<ApiType extends KubernetesObject> implements BeanCreatedEventListener<ResourceReconciler<ApiType>> {
    public static final Logger LOG = LoggerFactory.getLogger(ResourceReconcilerCreatedListener.class);

    private final BeanContext beanContext;
    private final ControllerFactory controllerFactory;

    public ResourceReconcilerCreatedListener(BeanContext beanContext, @NonNull ControllerFactory controllerFactory) {
        this.beanContext = beanContext;
        this.controllerFactory = controllerFactory;
    }

    @Override
    public ResourceReconciler<ApiType> onCreated(BeanCreatedEvent<ResourceReconciler<ApiType>> event) {
        ResourceReconciler<ApiType> resourceReconciler = event.getBean();

        BeanDefinition<ResourceReconciler<ApiType>> beanDefinition = event.getBeanDefinition();
        if (beanDefinition.hasAnnotation(Operator.class)) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Found @Operator annotation on {}", resourceReconciler);
            }

            final AnnotationValue<Operator> annotationValue = beanDefinition.getAnnotationMetadata().getAnnotation(Operator.class);
            final ControllerConfiguration controllerConfiguration = beanContext.createBean(ControllerConfiguration.class, annotationValue);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Final controller configuration: {}", controllerConfiguration);
            }

            controllerFactory.createControllers(resourceReconciler, controllerConfiguration);
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Bean [{}] implements ResourceReconciler but " +
                        "the io.micronaut.kubernetes.client.operator.@Operator annotation is missing.", resourceReconciler);
            }
        }
        return resourceReconciler;
    }
}
