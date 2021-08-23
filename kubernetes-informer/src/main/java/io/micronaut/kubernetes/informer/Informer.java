package io.micronaut.kubernetes.informer;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.micronaut.aop.AroundConstruct;
import io.micronaut.context.annotation.Prototype;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Pavol Gressa
 * @since 2.5
 */
@Retention(RetentionPolicy.RUNTIME)
@AroundConstruct
@Prototype
public @interface Informer {

    Class<? extends KubernetesObject> apiTypeClass();

    Class<? extends KubernetesListObject> apiListTypeClass();


}
