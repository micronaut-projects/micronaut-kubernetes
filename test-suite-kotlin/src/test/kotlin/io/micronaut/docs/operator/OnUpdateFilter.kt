package io.micronaut.docs.operator
//tag::reconciler[]
import io.kubernetes.client.openapi.models.V1ConfigMap
import jakarta.inject.Singleton

import java.util.function.BiPredicate

@Singleton
class OnUpdateFilter : BiPredicate<V1ConfigMap, V1ConfigMap> {

    override fun test(oldObj: V1ConfigMap, newObj: V1ConfigMap): Boolean {
        val annotations = newObj.metadata?.annotations
        if (annotations != null) {
            return annotations.containsKey("io.micronaut.operator")
        }
        return false
    }
}
//end::reconciler[]
