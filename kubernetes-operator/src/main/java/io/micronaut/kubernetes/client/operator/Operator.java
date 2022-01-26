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
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.DefaultScope;
import io.micronaut.kubernetes.client.informer.Informer;
import io.micronaut.kubernetes.client.operator.filter.DefaultAddFilter;
import io.micronaut.kubernetes.client.operator.filter.DefaultDeleteFilter;
import io.micronaut.kubernetes.client.operator.filter.DefaultUpdateFilter;

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
 * @since 3.3
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Bean
@DefaultScope(Context.class)
public @interface Operator {

    /**
     * The name of the operator. The name is used to uniquely identify created controllers in the context. If not provided
     * the controller name is generated.
     *
     * @return operator name
     */
    String name() default "";

    Informer informer();

    Class<? extends Predicate<? extends KubernetesObject>> onAddFilter() default DefaultAddFilter.class;

    Class<? extends BiPredicate<? extends KubernetesObject, ? extends KubernetesObject>> onUpdateFilter() default DefaultUpdateFilter.class;

    Class<? extends BiPredicate<? extends KubernetesObject, Boolean>> onDeleteFilter() default DefaultDeleteFilter.class;
}
