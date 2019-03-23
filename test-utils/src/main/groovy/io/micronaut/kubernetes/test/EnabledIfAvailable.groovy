package io.micronaut.kubernetes.test

import org.junit.jupiter.api.extension.ExtendWith

import java.lang.annotation.Retention
import java.lang.annotation.Target

import static java.lang.annotation.ElementType.*
import static java.lang.annotation.RetentionPolicy.RUNTIME

@Target([METHOD, TYPE, ANNOTATION_TYPE ])
@Retention(RUNTIME)
@ExtendWith(EnabledIfAvailableCondition)
@interface EnabledIfAvailable {

    String value()

}
