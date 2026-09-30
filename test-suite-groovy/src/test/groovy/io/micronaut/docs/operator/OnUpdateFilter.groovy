package io.micronaut.docs.operator
//tag::reconciler[]
import io.kubernetes.client.openapi.models.V1ConfigMap
import jakarta.inject.Singleton

import java.util.function.BiPredicate

@Singleton
class OnUpdateFilter implements BiPredicate<V1ConfigMap, V1ConfigMap> {

    @Override
    boolean test(V1ConfigMap oldObj, V1ConfigMap newObj) {
        if (newObj.metadata.annotations != null) {
            return newObj.metadata.annotations.containsKey("io.micronaut.operator")
        }
        return false
    }
}
//end::reconciler[]
