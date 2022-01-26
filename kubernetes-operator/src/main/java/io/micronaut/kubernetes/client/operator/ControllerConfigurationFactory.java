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
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.informer.Informer;
import io.micronaut.kubernetes.client.informer.resolvers.InformerApiGroupResolver;
import io.micronaut.kubernetes.client.informer.resolvers.InformerLabelSelectorResolver;
import io.micronaut.kubernetes.client.informer.resolvers.InformerNamespaceResolver;
import io.micronaut.kubernetes.client.informer.resolvers.InformerResourcePluralResolver;
import io.micronaut.kubernetes.client.informer.InformerAnnotationUtils;

import javax.naming.ConfigurationException;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Factory for the {@link ControllerConfiguration}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Factory
public class ControllerConfigurationFactory {

    private final BeanContext beanContext;
    private final InformerApiGroupResolver apiGroupResolver;
    private final InformerResourcePluralResolver resourcePluralResolver;
    private final InformerNamespaceResolver namespaceResolver;
    private final InformerLabelSelectorResolver labelSelectorResolver;

    public ControllerConfigurationFactory(@NonNull BeanContext beanContext,
                                          @NonNull InformerApiGroupResolver apiGroupResolver,
                                          @NonNull InformerResourcePluralResolver resourcePluralResolver,
                                          @NonNull InformerNamespaceResolver namespaceResolver,
                                          @NonNull InformerLabelSelectorResolver labelSelectorResolver) {
        this.beanContext = beanContext;
        this.apiGroupResolver = apiGroupResolver;
        this.resourcePluralResolver = resourcePluralResolver;
        this.namespaceResolver = namespaceResolver;
        this.labelSelectorResolver = labelSelectorResolver;
    }

    /**
     * Creates the {@link ControllerConfiguration} from the {@link Operator} annotation.
     *
     * @param annotationValue operator annotation value
     * @return operator configuration
     * @throws ConfigurationException when required configuration is missing
     */
    @Prototype
    public ControllerConfiguration operatorConfiguration(@Parameter AnnotationValue<Operator> annotationValue) throws ConfigurationException {

        Optional<AnnotationValue<Informer>> optionalInformer = annotationValue.getAnnotation("informer", Informer.class);
        if (!optionalInformer.isPresent()) {
            throw new ConfigurationException("Failed to configure the Operator Controller. The @Informer is required.");
        }
        AnnotationValue<Informer> informer = optionalInformer.get();

        final Class<? extends KubernetesObject> apiType = InformerAnnotationUtils.resolveApiType(informer);
        final String name = annotationValue.get("name", String.class).orElseGet(() -> "Operator" + apiType.getSimpleName());

        ControllerConfigurationImpl.ControllerConfigurationBuilder builder = new ControllerConfigurationImpl.ControllerConfigurationBuilder();
        return builder.withApiType(apiType)
                .withApiListType(InformerAnnotationUtils.resolveApiListType(informer))
                .withApiGroup(apiGroupResolver.resolveInformerApiGroup(informer))
                .withResourcePlural(resourcePluralResolver.resolveInformerResourcePlural(informer))
                .withNamespaces(namespaceResolver.resolveInformerNamespaces(informer))
                .withLabelSelector(labelSelectorResolver.resolveInformerLabels(informer))
                .withOnAddFilter(getOnAddFilter(annotationValue))
                .withOnUpdateFilter(getOnUpdateFilter(annotationValue))
                .withOnDeleteFilter(getOnDeleteFilter(annotationValue))
                .withName(name)
                .withResyncCheckPeriod(informer.get("resyncCheckPeriod", Long.class).orElse(0L))
                .build();
    }

    /**
     * Resolve the onAddFilter from the annotation value.
     *
     * @param operator annotation value
     * @return the on add filter predicate
     */
    protected Predicate<? extends KubernetesObject> getOnAddFilter(AnnotationValue<Operator> operator) {
        Optional<Class<? extends Predicate>> onAddFilter = operator.classValue("onAddFilter", Predicate.class);
        return onAddFilter.map(beanContext::getBean).orElse(null);
    }

    /**
     * Resolve the onUpdateFilter from the annotation value.
     *
     * @param operator annotation value
     * @return the on update filter bi predicate
     */
    protected BiPredicate<? extends KubernetesObject, ? extends KubernetesObject> getOnUpdateFilter(AnnotationValue<Operator> operator) {
        Optional<Class<? extends BiPredicate>> onUpdateFilter = operator.classValue("onUpdateFilter", BiPredicate.class);
        return onUpdateFilter.map(beanContext::getBean).orElse(null);
    }

    /**
     * Resolve the onDeleteFilter from the annotation value.
     *
     * @param operator annotation value
     * @return the on delete filter bi predicate
     */
    protected BiPredicate<? extends KubernetesObject, Boolean> getOnDeleteFilter(AnnotationValue<Operator> operator) {
        Optional<Class<? extends BiPredicate>> onDeleteFilter = operator.classValue("onDeleteFilter", BiPredicate.class);
        return onDeleteFilter.map(beanContext::getBean).orElse(null);
    }
}
