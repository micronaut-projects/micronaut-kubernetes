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

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.micronaut.aop.AroundConstruct;
import io.micronaut.context.annotation.Prototype;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;

/**
 * Annotation used in combination with {@link io.kubernetes.client.informer.ResourceEventHandler} will cause the
 * {@link io.kubernetes.client.informer.SharedIndexInformer} will be created by {@link ResourceEventHandlerConstructorInterceptor}.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@AroundConstruct
@Prototype
public @interface Informer {

    String ALL_NAMESPACES = "ALL_NAMESPACES";

    /**
     * The resource type.
     *
     * @return resource type.
     */
    Class<? extends KubernetesObject> apiType();

    /**
     * The resource list type.
     *
     * @return list type
     */
    Class<? extends KubernetesListObject> apiListType();

    /**
     * The watched resource plural. For example for {@link io.kubernetes.client.openapi.models.V1ConfigMap} it
     * is {@code configmaps}. To find the proper resource plural visit the <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.22/">Kubernetes API Reference</a>.
     *
     * @return resource plural
     */
    String resourcePlural();

    /**
     * Namespace of the watched resource. If empty then namespace is resolved
     * by {@link io.micronaut.kubernetes.client.NamespaceResolver}. To watch resources
     * from all namespaces configure this parameter to {@link Informer#ALL_NAMESPACES}.
     *
     * @return namespace name
     */
    String namespace() default "";

    /**
     * How often to check if the listener need a resync.
     *
     * @return resync check period, if 0L returned then default minimal resync interval is used
     * @see io.kubernetes.client.informer.impl.DefaultSharedIndexInformer
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
