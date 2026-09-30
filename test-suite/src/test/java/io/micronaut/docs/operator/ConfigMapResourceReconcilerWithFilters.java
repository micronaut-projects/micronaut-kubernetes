package io.micronaut.docs.operator;
//tag::reconciler[]

import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.micronaut.kubernetes.client.informer.Informer;
import io.micronaut.kubernetes.client.operator.Operator;
import io.micronaut.kubernetes.client.operator.OperatorResourceLister;
import io.micronaut.kubernetes.client.operator.ResourceReconciler;
//end::reconciler[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.kubernetes.client.operator.event.LeaseAcquiredEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.atomic.AtomicBoolean;

@Requires(property = "spec.name", value = "ConfigMapResourceReconcilerWithFiltersSpec")
//tag::reconciler[]

@Operator(informer = @Informer(apiType = V1ConfigMap.class, apiListType = V1ConfigMapList.class),
        onAddFilter = OnAddFilter.class, // <1>
        onUpdateFilter = OnUpdateFilter.class, // <2>
        onDeleteFilter = OnDeleteFilter.class) // <3>
public class ConfigMapResourceReconcilerWithFilters implements ResourceReconciler<V1ConfigMap> {

    //end::reconciler[]
    List<String> requestList = new ArrayList<>();
    AtomicBoolean leaseAcquired = new AtomicBoolean(false);

    //tag::reconciler[]
    @Override
    public Result reconcile(Request request, OperatorResourceLister<V1ConfigMap> lister) {
        // .. reconcile
        //end::reconciler[]
        requestList.add(request.getName());
        //tag::reconciler[]
        return new Result(false);
    }
    //end::reconciler[]

    @EventListener
    public void onBecomingLeader(LeaseAcquiredEvent leaseAcquiredEvent) {
        leaseAcquired.set(true);
    }
//tag::reconciler[]
}
//end::reconciler[]
