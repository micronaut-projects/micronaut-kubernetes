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

import io.micronaut.aop.AroundConstruct;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.DefaultScope;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

/**
 * Annotation used in combination with {@link ResourceEventHandler} will cause
 * {@link SharedIndexInformer} be created by {@link ResourceEventHandlerBeanListener}.
 */
@Retention(RetentionPolicy.RUNTIME)
@AroundConstruct
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Bean
@DefaultScope(Context.class)
public @interface Informer {

    String ALL_NAMESPACES = "ALL_NAMESPACES";
    String RESOLVE_AUTOMATICALLY = "RESOLVE_AUTOMATICALLY";

    /**
     * The resource type.
     *
     * @return resource type.
     */
    Class<? extends KubernetesObject> apiType();

    /**
     * Watched resource namespace. If empty then namespace is resolved by {@link InformerNamespaceResolver}.
     * To watch resources from all namespaces configure this parameter to {@link Informer#ALL_NAMESPACES}.
     *
     * @return namespace name
     */
    String namespace() default RESOLVE_AUTOMATICALLY;

    /**
     * Watched resource namespaces. If empty then namespace is resolved by {@link InformerNamespaceResolver}.
     *
     * @return array of namespace names
     */
    String[] namespaces() default {};

    /**
     * Namespaces supplier bean class.
     *
     * @return supplier class
     */
    Class<? extends Supplier<String[]>> namespacesSupplier() default EmptyNamespacesSupplier.class;

    /**
     * Period in milliseconds which defines how often to check whether the listener need a resync.
     *
     * @return resync check period, if 0L returned then default minimal resync interval is used
     */
    long resyncCheckPeriod() default 0L;

    /**
     * Informer label selector.
     *
     * @return label selector
     * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">Label selectors</a>
     */
    String labelSelector() default "";

    /**
     * Informer label selector supplier.
     *
     * @return label selector supplier
     * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">Label selectors</a>
     */
    Class<? extends Supplier<String>> labelSelectorSupplier() default EmptyLabelSupplier.class;
}
