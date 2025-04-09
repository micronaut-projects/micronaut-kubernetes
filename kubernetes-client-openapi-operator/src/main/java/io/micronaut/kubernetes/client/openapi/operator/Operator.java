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

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.DefaultScope;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Operator annotation simplifies initialisation of the controllers.
 *
 * @author Pavol Gressa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Bean
@DefaultScope(Context.class)
@SuppressWarnings("java:S1452")
public @interface Operator {

    /**
     * The name of the operator. It is used to uniquely identify the created controller. If not provided
     * the controller name is generated.
     *
     * @return operator name
     */
    String name() default "";

    /**
     * The informer which is used to watch and report resource changes.
     *
     * @return the informer instance
     */
    Informer informer();

    /**
     * The class of the filter applied by informer's resource handler when a new resource is created.
     *
     * @return filter class
     */
    Class<? extends Predicate<? extends KubernetesObject>> onAddFilter() default OperatorFilter.OnAdd.class;

    /**
     * The class of the filter applied by informer's resource handler when an existing resource is updated.
     *
     * @return filter class
     */
    Class<? extends BiPredicate<? extends KubernetesObject, ? extends KubernetesObject>> onUpdateFilter() default OperatorFilter.OnUpdate.class;

    /**
     * The class of the filter applied by informer's resource handler when an existing resource is deleted.
     *
     * @return filter class
     */
    Class<? extends BiPredicate<? extends KubernetesObject, Boolean>> onDeleteFilter() default OperatorFilter.OnDelete.class;
}
