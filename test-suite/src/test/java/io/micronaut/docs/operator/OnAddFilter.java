package io.micronaut.docs.operator;
//tag::reconciler[]
import io.kubernetes.client.openapi.models.V1ConfigMap;
import jakarta.inject.Singleton;

import java.util.function.Predicate;

@Singleton
public class OnAddFilter implements Predicate<V1ConfigMap> {

    @Override
    public boolean test(V1ConfigMap v1ConfigMap) {
        if (v1ConfigMap.getMetadata().getAnnotations() != null) {
            return v1ConfigMap.getMetadata().getAnnotations().containsKey("io.micronaut.operator");
        }
        return false;
    }
}
//end::reconciler[]
