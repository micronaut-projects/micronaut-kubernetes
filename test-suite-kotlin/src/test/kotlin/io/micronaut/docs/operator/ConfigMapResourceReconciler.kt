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

@Operator(informer = Informer(apiType = V1ConfigMap::class, apiListType = V1ConfigMapList::class)) // <1>
class ConfigMapResourceReconciler : ResourceReconciler<V1ConfigMap> { // <2>

    //end::reconciler[]
    val requestList: MutableList<String> = ArrayList()
    val leaseAcquired = AtomicBoolean(false)

    //tag::reconciler[]
    override fun reconcile(request: Request, lister: OperatorResourceLister<V1ConfigMap>): Result { // <3>
        val resource = lister.get(request) // <4>
        // .. reconcile  <5>
        //end::reconciler[]
        resource.ifPresent { v1ConfigMap ->
            requestList.add(v1ConfigMap.metadata!!.name!!)
        }
        //tag::reconciler[]
        return Result(false) // <6>
    }
    //end::reconciler[]

    @EventListener
    fun onBecomingLeader(leaseAcquiredEvent: LeaseAcquiredEvent) {
        leaseAcquired.set(true)
    }
//tag::reconciler[]
}
//end::reconciler[]
