package micronaut.operator

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.operator.Operator
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result
import org.jspecify.annotations.NonNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Only one informer per resource type and namespace can be registered, so this reconciler is
// enabled instead of ConfigMapReconciler by setting the example.config-map-filters property
@CompileStatic
@Requires(property = "example.config-map-filters", value = "true")
//tag::operator[]
@Operator(informer = @Informer(apiType = V1ConfigMap),
        onAddFilter = OnAddFilter,
        onUpdateFilter = OnUpdateFilter,
        onDeleteFilter = OnDeleteFilter)
class ConfigMapReconcilerWithFilters implements ResourceReconciler<V1ConfigMap> {
//end::operator[]

    private static final Logger LOG = LoggerFactory.getLogger(ConfigMapReconcilerWithFilters)

    @Override
    @NonNull
    Result reconcile(@NonNull Request request, @NonNull OperatorResourceLister<V1ConfigMap> lister) {
        LOG.info("Reconciling annotated config map: {}", request)
        // .. reconcile
        return new Result(false)
    }
//tag::operator[]
}
//end::operator[]
