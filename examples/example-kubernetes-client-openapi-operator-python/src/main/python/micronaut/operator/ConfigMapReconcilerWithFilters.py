import logging

from micronaut.context.annotation import Requires
from micronaut.context.env import Environment
from micronaut.kubernetes.client.openapi.informer.handler import Informer
from micronaut.kubernetes.client.openapi.model import V1ConfigMap
from micronaut.kubernetes.client.openapi.operator import Operator, OperatorResourceLister
from micronaut.kubernetes.client.openapi.operator.controller.reconciler import Request, ResourceReconciler, Result

from micronaut.operator.OnAddFilter import OnAddFilter
from micronaut.operator.OnDeleteFilter import OnDeleteFilter
from micronaut.operator.OnUpdateFilter import OnUpdateFilter

LOG = logging.getLogger(__name__)


# Only one informer per resource type and namespace can be registered, so this reconciler is
# enabled instead of ConfigMapReconciler by setting the example.config-map-filters property
@Requires(env=Environment.KUBERNETES)
@Requires(property="example.config-map-filters", value="true")
# tag::operator[]
@Operator(informer=Informer(apiType=V1ConfigMap),
          onAddFilter=OnAddFilter,
          onUpdateFilter=OnUpdateFilter,
          onDeleteFilter=OnDeleteFilter)
class ConfigMapReconcilerWithFilters(ResourceReconciler[V1ConfigMap]):
    # end::operator[]

    def reconcile(self, request: Request, lister: OperatorResourceLister[V1ConfigMap]) -> Result:
        LOG.info("Reconciling annotated config map: %s", request)
        # .. reconcile
        return Result(False)
    # tag::operator[]
    ...
# end::operator[]
