# tag::reconciler[]
from java.util import Optional
from micronaut.kubernetes.client.informer import Informer
from micronaut.kubernetes.client.operator import Operator, OperatorResourceLister, ResourceReconciler
# end::reconciler[]
from micronaut.context.annotation import Requires
from micronaut.kubernetes.client.operator.event import LeaseAcquiredEvent
from micronaut.runtime.event.annotation import EventListener
# tag::reconciler[]

try:
    from io.kubernetes.client.extended.controller.reconciler import Request, Result
    from io.kubernetes.client.openapi.models import V1ConfigMap, V1ConfigMapList
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from kubernetes.client.extended.controller.reconciler import Request, Result
    from kubernetes.client.openapi.models import V1ConfigMap, V1ConfigMapList

# end::reconciler[]

@Requires(property="spec.name", value="ConfigMapResourceReconcilerSpec")
# tag::reconciler[]
@Operator(informer=Informer(apiType=V1ConfigMap, apiListType=V1ConfigMapList))  # <1>
class ConfigMapResourceReconciler(ResourceReconciler[V1ConfigMap]):  # <2>

    # end::reconciler[]
    def __init__(self):
        self.request_list: list[str] = []
        self.lease_acquired = False

    # tag::reconciler[]
    def reconcile(self, request: Request, lister: OperatorResourceLister[V1ConfigMap]) -> Result:  # <3>
        resource: Optional[V1ConfigMap] = lister.get(request)  # <4>
        # .. reconcile  <5>
        # end::reconciler[]
        if resource.isPresent():
            self.request_list.append(resource.get().getMetadata().getName())
        # tag::reconciler[]
        return Result(False)  # <6>
    # end::reconciler[]

    @EventListener
    def on_becoming_leader(self, lease_acquired_event: LeaseAcquiredEvent) -> None:
        self.lease_acquired = True
# tag::reconciler[]
# end::reconciler[]
