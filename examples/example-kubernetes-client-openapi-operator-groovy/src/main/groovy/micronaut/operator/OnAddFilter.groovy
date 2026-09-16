//tag::class[]
package micronaut.operator

import groovy.transform.CompileStatic
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import jakarta.inject.Singleton

import java.util.function.Predicate

@CompileStatic
@Singleton
class OnAddFilter implements Predicate<V1ConfigMap> {
    @Override
    boolean test(V1ConfigMap configMap) {
        return configMap.metadata.annotations != null
            && configMap.metadata.annotations.containsKey("io.micronaut.operator")
    }
}
//end::class[]
