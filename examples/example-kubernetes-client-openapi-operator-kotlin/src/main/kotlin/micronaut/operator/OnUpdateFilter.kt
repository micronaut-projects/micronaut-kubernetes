//tag::class[]
package micronaut.operator

import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import jakarta.inject.Singleton
//end::class[]
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
//tag::class[]

import java.util.function.BiPredicate

//end::class[]
@Requires(env = [Environment.KUBERNETES])
//tag::class[]
@Singleton
class OnUpdateFilter : BiPredicate<V1ConfigMap, V1ConfigMap> {
    override fun test(oldConfigMap: V1ConfigMap, newConfigMap: V1ConfigMap): Boolean {
        val annotations = newConfigMap.metadata?.annotations
        return annotations != null && annotations.containsKey("io.micronaut.operator")
    }
}
//end::class[]
