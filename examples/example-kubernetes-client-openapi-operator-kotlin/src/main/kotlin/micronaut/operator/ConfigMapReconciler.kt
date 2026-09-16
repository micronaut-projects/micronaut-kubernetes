//tag::reconciler[]
package micronaut.operator

//end::reconciler[]
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
//tag::reconciler[]
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
//end::reconciler[]
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
//tag::reconciler[]
import io.micronaut.kubernetes.client.openapi.operator.Operator
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result
//end::reconciler[]
import org.slf4j.LoggerFactory

import java.time.Duration
//tag::reconciler[]
import java.util.Optional

//end::reconciler[]
@Requires(env = [Environment.KUBERNETES])
//tag::reconciler[]
@Operator(informer = Informer(apiType = V1ConfigMap::class)) // <1>
class ConfigMapReconciler(private val coreV1Api: CoreV1Api) : ResourceReconciler<V1ConfigMap> { // <2>

    //end::reconciler[]

    companion object {
        private val LOG = LoggerFactory.getLogger(ConfigMapReconciler::class.java)
    }

    //tag::reconciler[]
    override fun reconcile(request: Request, lister: OperatorResourceLister<V1ConfigMap>): Result { // <3>
        val configMapOpt: Optional<V1ConfigMap> = lister.get(request) // <4>
        // .. reconcile  <5>
        //end::reconciler[]
        LOG.info("Reconciling config map: {}", request)
        if (configMapOpt.isPresent) {
            val configMap = configMapOpt.get()
            val metadata: V1ObjectMeta = configMap.metadata!!

            var annotations = metadata.annotations
            if (annotations == null) {
                annotations = HashMap()
                metadata.annotations = annotations
            }

            if (!annotations.containsKey("io.micronaut.operator")) {
                annotations["io.micronaut.operator"] = "processed"
                val name = metadata.name!!
                val namespace = metadata.namespace!!
                try {
                    coreV1Api.replaceNamespacedConfigMap(name, namespace, configMap, null, null, null, null)
                } catch (e: Exception) {
                    LOG.error("Failed to update config map", e)
                    return Result(true, Duration.ofSeconds(2))
                }
            }
        }
        //tag::reconciler[]
        return Result(false) // <6>
    }
}
//end::reconciler[]
