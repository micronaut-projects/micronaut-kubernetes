package micronaut.operator;

import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import jakarta.inject.Singleton;

import java.util.function.BiPredicate;

@Singleton
public class OnDeleteFilter implements BiPredicate<V1ConfigMap, Boolean> {
    @Override
    public boolean test(V1ConfigMap configMap, Boolean deletedFinalStateUnknown) {
        return configMap.getMetadata().getAnnotations() != null
            && configMap.getMetadata().getAnnotations().containsKey("io.micronaut.operator");
    }
}
