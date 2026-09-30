//tag::class[]
package micronaut.operator

import groovy.transform.CompileStatic
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import jakarta.inject.Singleton

import java.util.function.BiPredicate

@CompileStatic
@Singleton
class OnUpdateFilter implements BiPredicate<V1ConfigMap, V1ConfigMap> {
    @Override
    boolean test(V1ConfigMap oldConfigMap, V1ConfigMap newConfigMap) {
        return newConfigMap.metadata.annotations != null
            && newConfigMap.metadata.annotations.containsKey("io.micronaut.operator")
    }
}
//end::class[]
