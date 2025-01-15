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

import io.micronaut.aop.AroundConstruct;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.DefaultScope;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used in combination with {@link ResourceEventHandler} will cause the
 * {@link SharedIndexInformer} be created by {@link ResourceEventHandlerBeanListener}.
 */
@Retention(RetentionPolicy.RUNTIME)
@AroundConstruct
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Bean
@DefaultScope(Context.class)
public @interface Informer {

    /**
     * The resource type.
     *
     * @return resource type.
     */
    Class<? extends KubernetesObject> apiType();


    /**
     * Watched resource namespace.
     *
     * @return namespace
     */
    String namespace() default "";

    /**
     * Period in milliseconds which defines how often to check whether the listener need a resync.
     *
     * @return resync check period, if 0L returned then default minimal resync interval is used
     */
    long resyncCheckPeriod() default 0L;
}
