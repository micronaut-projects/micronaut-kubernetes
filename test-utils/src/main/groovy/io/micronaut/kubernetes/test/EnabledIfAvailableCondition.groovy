package io.micronaut.kubernetes.test

import groovy.transform.CompileStatic
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

import java.lang.reflect.AnnotatedElement

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation

@CompileStatic
class EnabledIfAvailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@EnabledIfAvailable is not present")

    @Override
    ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        AnnotatedElement element = context
                .getElement()
                .orElseThrow{ new IllegalStateException() }

        return findAnnotation(element, EnabledIfAvailable)
                .map{annotation -> disableIfUnreachable(annotation, element)}
                .orElse(ENABLED_BY_DEFAULT)
    }

    private ConditionEvaluationResult disableIfUnreachable(EnabledIfAvailable annotation, AnnotatedElement element) {
        String url = annotation.value()
        boolean reachable = TestUtils.available(url)
        if (reachable) {
            return enabled(String.format("%s is enabled because %s is reachable", element, url))
        } else {
            return disabled(String.format("%s is disabled because %s could not be reached", element, url))
        }
    }

}
