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

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * The operator controller configuration.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
public interface ControllerConfiguration {

    /**
     * The operator controller name. The name is used to uniquely identify the operator in the application context.
     *
     * @return name
     */
    @NonNull
    String getName();

    /**
     * The api resource type the operator controller reconciles.
     *
     * @return api resource type
     */
    @NonNull
    Class<? extends KubernetesObject> getApiType();

    /**
     * The api resource list type the operator controller reconciles.
     *
     * @return api resource list type
     */
    @NonNull
    Class<? extends KubernetesListObject> getApiListType();

    /**
     * The api resource plural the operator controller reconciles.
     *
     * @return api resource plural
     */
    @NonNull
    String getResourcePlural();

    /**
     * The api resource group the operator controller reconciles.
     *
     * @return api group
     */
    @NonNull
    String getApiGroup();

    /**
     * The namespaces from which the operator controller receives the resources for reconciliation.
     *
     * @return namespaces
     */
    @NonNull
    Set<String> getNamespaces();

    /**
     * The operator's informer label selector.
     *
     * @return label selector
     * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">Label selectors</a>
     */
    @NonNull
    String getLabelSelector();

    /**
     * How often to check if the listener need a resync.
     *
     * @return resync check period, if 0L returned then default minimal resync interval is used
     * @see io.kubernetes.client.informer.impl.DefaultSharedIndexInformer
     */
    @NonNull
    Long getResyncCheckPeriod();

    /**
     * Predicate that filters added resources before reconciliation.
     * Default {@link io.micronaut.kubernetes.client.operator.filter.DefaultAddFilter}.
     *
     * @return predicate
     */
    @Nullable
    Predicate<? extends KubernetesObject> getOnAddFilter();

    /**
     * Predicate that filters updated resources before reconciliation.
     * Default {@link io.micronaut.kubernetes.client.operator.filter.DefaultUpdateFilter}.
     *
     * @return bipredicate
     */
    @Nullable
    BiPredicate<? extends KubernetesObject, ? extends KubernetesObject> getOnUpdateFilter();

    /**
     * Predicate that filters deleted resources before reconciliation.
     * Default {@link io.micronaut.kubernetes.client.operator.filter.DefaultDeleteFilter}.
     *
     * @return bipredicate
     */
    @Nullable
    BiPredicate<? extends KubernetesObject, Boolean> getOnDeleteFilter();
}
