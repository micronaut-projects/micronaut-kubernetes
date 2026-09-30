package io.micronaut.docs.operator
//tag::reconciler[]
import io.kubernetes.client.openapi.models.V1ConfigMap
import jakarta.inject.Singleton

import java.util.function.Predicate

@Singleton
class OnAddFilter implements Predicate<V1ConfigMap> {

    @Override
    boolean test(V1ConfigMap v1ConfigMap) {
        if (v1ConfigMap.metadata.annotations != null) {
            return v1ConfigMap.metadata.annotations.containsKey("io.micronaut.operator")
        }
        return false
    }
}
//end::reconciler[]
