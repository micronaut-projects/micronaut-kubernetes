# tag::reconciler[]
import logging

from java.util import Optional
# end::reconciler[]
from java.time import Duration
from java.util import HashMap
from micronaut.context.annotation import Requires
from micronaut.context.env import Environment
from micronaut.kubernetes.client.openapi.api import CoreV1Api
# tag::reconciler[]
from micronaut.kubernetes.client.openapi.informer.handler import Informer
from micronaut.kubernetes.client.openapi.model import V1ConfigMap
from micronaut.kubernetes.client.openapi.operator import Operator, OperatorResourceLister
from micronaut.kubernetes.client.openapi.operator.controller.reconciler import Request, ResourceReconciler, Result
# end::reconciler[]

LOG = logging.getLogger(__name__)


@Requires(env=Environment.KUBERNETES)
# tag::reconciler[]
@Operator(informer=Informer(apiType=V1ConfigMap))  # <1>
class ConfigMapReconciler(ResourceReconciler[V1ConfigMap]):  # <2>

    # end::reconciler[]
    def __init__(self, core_v1_api: CoreV1Api):
        self.core_v1_api = core_v1_api

    # tag::reconciler[]
    def reconcile(self, request: Request, lister: OperatorResourceLister[V1ConfigMap]) -> Result:  # <3>
        config_map_opt: Optional[V1ConfigMap] = lister.get(request)  # <4>
        # .. reconcile  <5>
        # end::reconciler[]
        LOG.info("Reconciling config map: %s", request)
        if config_map_opt.isPresent():
            config_map = config_map_opt.get()
            metadata = config_map.getMetadata()

            annotations = metadata.getAnnotations()
            if annotations is None:
                annotations = HashMap()
                metadata.setAnnotations(annotations)

            if "io.micronaut.operator" not in annotations:
                annotations["io.micronaut.operator"] = "processed"
                name = metadata.getName()
                namespace = metadata.getNamespace()
                try:
                    self.core_v1_api.replaceNamespacedConfigMap(name, namespace, config_map, None, None, None, None)
                except Exception as e:
                    LOG.exception("Failed to update config map")
                    return Result(True, Duration.ofSeconds(2))
        # tag::reconciler[]
        return Result(False)  # <6>
# end::reconciler[]
