package io.micronaut.docs.operator;
//tag::reconciler[]
import io.kubernetes.client.openapi.models.V1ConfigMap;
import jakarta.inject.Singleton;

import java.util.function.BiPredicate;

@Singleton
public class OnDeleteFilter implements BiPredicate<V1ConfigMap, Boolean> {

    @Override
    public boolean test(V1ConfigMap v1ConfigMap, Boolean deletedFinalStateUnknown) {
        if (v1ConfigMap.getMetadata().getAnnotations() != null) {
            return v1ConfigMap.getMetadata().getAnnotations().containsKey("io.micronaut.operator");
        }
        return false;
    }
}
//end::reconciler[]
