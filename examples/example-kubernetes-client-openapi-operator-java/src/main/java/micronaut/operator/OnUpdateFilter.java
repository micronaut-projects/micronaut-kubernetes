package micronaut.operator;

import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import jakarta.inject.Singleton;

import java.util.function.BiPredicate;

@Singleton
public class OnUpdateFilter implements BiPredicate<V1ConfigMap, V1ConfigMap> {
    @Override
    public boolean test(V1ConfigMap oldConfigMap, V1ConfigMap newConfigMap) {
        return newConfigMap.getMetadata().getAnnotations() != null
            && newConfigMap.getMetadata().getAnnotations().containsKey("io.micronaut.operator");
    }
}
