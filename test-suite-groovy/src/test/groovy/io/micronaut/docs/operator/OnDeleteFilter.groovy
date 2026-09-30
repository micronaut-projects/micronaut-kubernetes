package io.micronaut.docs.operator
//tag::reconciler[]
import io.kubernetes.client.openapi.models.V1ConfigMap
import jakarta.inject.Singleton

import java.util.function.BiPredicate

@Singleton
class OnDeleteFilter implements BiPredicate<V1ConfigMap, Boolean> {

    @Override
    boolean test(V1ConfigMap v1ConfigMap, Boolean deletedFinalStateUnknown) {
        if (v1ConfigMap.metadata.annotations != null) {
            return v1ConfigMap.metadata.annotations.containsKey("io.micronaut.operator")
        }
        return false
    }
}
//end::reconciler[]
