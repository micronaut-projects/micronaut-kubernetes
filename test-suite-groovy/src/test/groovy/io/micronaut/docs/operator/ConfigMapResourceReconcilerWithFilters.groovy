package io.micronaut.docs.operator
//tag::reconciler[]

import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.extended.controller.reconciler.Result
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.kubernetes.client.informer.Informer
import io.micronaut.kubernetes.client.operator.Operator
import io.micronaut.kubernetes.client.operator.OperatorResourceLister
import io.micronaut.kubernetes.client.operator.ResourceReconciler
//end::reconciler[]
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.operator.event.LeaseAcquiredEvent
import io.micronaut.runtime.event.annotation.EventListener

import java.util.concurrent.atomic.AtomicBoolean

@Requires(property = "spec.name", value = "ConfigMapResourceReconcilerWithFiltersSpec")
//tag::reconciler[]

@Operator(informer = @Informer(apiType = V1ConfigMap, apiListType = V1ConfigMapList),
        onAddFilter = OnAddFilter, // <1>
        onUpdateFilter = OnUpdateFilter, // <2>
        onDeleteFilter = OnDeleteFilter) // <3>
class ConfigMapResourceReconcilerWithFilters implements ResourceReconciler<V1ConfigMap> {

    //end::reconciler[]
    List<String> requestList = []
    AtomicBoolean leaseAcquired = new AtomicBoolean(false)

    //tag::reconciler[]
    @Override
    Result reconcile(Request request, OperatorResourceLister<V1ConfigMap> lister) {
        // .. reconcile
        //end::reconciler[]
        requestList.add(request.name)
        //tag::reconciler[]
        return new Result(false)
    }
    //end::reconciler[]

    @EventListener
    void onBecomingLeader(LeaseAcquiredEvent leaseAcquiredEvent) {
        leaseAcquired.set(true)
    }
//tag::reconciler[]
}
//end::reconciler[]
