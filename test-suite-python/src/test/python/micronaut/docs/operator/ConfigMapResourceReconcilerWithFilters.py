# tag::reconciler[]
from micronaut.kubernetes.client.informer import Informer
from micronaut.kubernetes.client.operator import Operator, OperatorResourceLister, ResourceReconciler

from micronaut.docs.operator.OnAddFilter import OnAddFilter
from micronaut.docs.operator.OnDeleteFilter import OnDeleteFilter
from micronaut.docs.operator.OnUpdateFilter import OnUpdateFilter
# end::reconciler[]
from micronaut.context.annotation import Requires
from micronaut.kubernetes.client.operator.event import LeaseAcquiredEvent
from micronaut.runtime.event.annotation import EventListener
# tag::reconciler[]

from io.kubernetes.client.extended.controller.reconciler import Request, Result
from io.kubernetes.client.openapi.models import V1ConfigMap, V1ConfigMapList

# end::reconciler[]

@Requires(property="spec.name", value="ConfigMapResourceReconcilerWithFiltersSpec")
# tag::reconciler[]
@Operator(informer=Informer(apiType=V1ConfigMap, apiListType=V1ConfigMapList),
          onAddFilter=OnAddFilter,  # <1>
          onUpdateFilter=OnUpdateFilter,  # <2>
          onDeleteFilter=OnDeleteFilter)  # <3>
class ConfigMapResourceReconcilerWithFilters(ResourceReconciler[V1ConfigMap]):

    # end::reconciler[]
    def __init__(self):
        self.request_list: list[str] = []
        self.lease_acquired = False

    # tag::reconciler[]
    def reconcile(self, request: Request, lister: OperatorResourceLister[V1ConfigMap]) -> Result:
        # .. reconcile
        # end::reconciler[]
        self.request_list.append(request.getName())
        # tag::reconciler[]
        return Result(False)
    # end::reconciler[]

    @EventListener
    def on_becoming_leader(self, lease_acquired_event: LeaseAcquiredEvent) -> None:
        self.lease_acquired = True
# tag::reconciler[]
# end::reconciler[]
