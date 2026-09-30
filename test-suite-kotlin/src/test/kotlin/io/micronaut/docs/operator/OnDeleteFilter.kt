package io.micronaut.docs.operator
//tag::reconciler[]
import io.kubernetes.client.openapi.models.V1ConfigMap
import jakarta.inject.Singleton

import java.util.function.BiPredicate

@Singleton
class OnDeleteFilter : BiPredicate<V1ConfigMap, Boolean> {

    override fun test(v1ConfigMap: V1ConfigMap, deletedFinalStateUnknown: Boolean): Boolean {
        val annotations = v1ConfigMap.metadata?.annotations
        if (annotations != null) {
            return annotations.containsKey("io.micronaut.operator")
        }
        return false
    }
}
//end::reconciler[]
