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

@Requires(property = "spec.name", value = "ConfigMapResourceReconcilerSpec")
//tag::reconciler[]

@Operator(informer = @Informer(apiType = V1ConfigMap, apiListType = V1ConfigMapList)) // <1>
class ConfigMapResourceReconciler implements ResourceReconciler<V1ConfigMap> { // <2>

    //end::reconciler[]
    List<String> requestList = []
    AtomicBoolean leaseAcquired = new AtomicBoolean(false)

    //tag::reconciler[]
    @Override
    Result reconcile(Request request, OperatorResourceLister<V1ConfigMap> lister) { // <3>
        Optional<V1ConfigMap> resource = lister.get(request) // <4>
        // .. reconcile  <5>
        //end::reconciler[]
        resource.ifPresent { V1ConfigMap v1ConfigMap ->
            requestList.add(v1ConfigMap.metadata.name)
        }
        //tag::reconciler[]
        return new Result(false) // <6>
    }
    //end::reconciler[]

    @EventListener
    void onBecomingLeader(LeaseAcquiredEvent leaseAcquiredEvent) {
        leaseAcquired.set(true)
    }
//tag::reconciler[]
}
//end::reconciler[]
