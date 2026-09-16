//tag::class[]
package micronaut.operator

import groovy.transform.CompileStatic
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import jakarta.inject.Singleton

import java.util.function.BiPredicate

@CompileStatic
@Singleton
class OnDeleteFilter implements BiPredicate<V1ConfigMap, Boolean> {
    @Override
    boolean test(V1ConfigMap configMap, Boolean deletedFinalStateUnknown) {
        return configMap.metadata.annotations != null
            && configMap.metadata.annotations.containsKey("io.micronaut.operator")
    }
}
//end::class[]
