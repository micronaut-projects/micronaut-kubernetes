package micronaut.operator

import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.operator.Operator
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result
import org.slf4j.LoggerFactory

// Only one informer per resource type and namespace can be registered, so this reconciler is
// enabled instead of ConfigMapReconciler by setting the example.config-map-filters property
@Requires(env = [Environment.KUBERNETES])
@Requires(property = "example.config-map-filters", value = "true")
//tag::operator[]
@Operator(informer = Informer(apiType = V1ConfigMap::class),
        onAddFilter = OnAddFilter::class,
        onUpdateFilter = OnUpdateFilter::class,
        onDeleteFilter = OnDeleteFilter::class)
class ConfigMapReconcilerWithFilters : ResourceReconciler<V1ConfigMap> {
//end::operator[]

    companion object {
        private val LOG = LoggerFactory.getLogger(ConfigMapReconcilerWithFilters::class.java)
    }

    override fun reconcile(request: Request, lister: OperatorResourceLister<V1ConfigMap>): Result {
        LOG.info("Reconciling annotated config map: {}", request)
        // .. reconcile
        return Result(false)
    }
//tag::operator[]
}
//end::operator[]
