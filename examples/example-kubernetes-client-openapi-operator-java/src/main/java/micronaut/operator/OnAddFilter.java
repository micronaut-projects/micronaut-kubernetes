package micronaut.operator;

import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import jakarta.inject.Singleton;

import java.util.function.Predicate;

@Singleton
public class OnAddFilter implements Predicate<V1ConfigMap> {
    @Override
    public boolean test(V1ConfigMap configMap) {
        return configMap.getMetadata().getAnnotations() != null
            && configMap.getMetadata().getAnnotations().containsKey("io.micronaut.operator");
    }
}
