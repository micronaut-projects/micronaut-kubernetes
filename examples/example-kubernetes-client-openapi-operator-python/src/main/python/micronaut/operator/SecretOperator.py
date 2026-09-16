# tag::class[]
import logging

from micronaut.context.annotation import Context
from micronaut.kubernetes.client.openapi.informer import SharedIndexInformerFactory
from micronaut.kubernetes.client.openapi.model import V1Secret
from micronaut.kubernetes.client.openapi.operator import OperatorResourceLister
from micronaut.kubernetes.client.openapi.operator.controller import ControllerFactory
from micronaut.kubernetes.client.openapi.operator.controller.reconciler import Request, ResourceReconciler, Result
from micronaut.kubernetes.client.openapi.resolver import NamespaceResolver
# end::class[]
from micronaut.context.annotation import Requires
from micronaut.context.env import Environment
# tag::class[]

LOG = logging.getLogger(__name__)

# end::class[]

@Requires(env=Environment.KUBERNETES)
# tag::class[]
@Context
class SecretOperator:

    def __init__(self, shared_index_informer_factory: SharedIndexInformerFactory, controller_factory: ControllerFactory, namespace_resolver: NamespaceResolver):
        namespace = namespace_resolver.resolveNamespace()
        shared_index_informer_factory.sharedIndexInformerFor(V1Secret, namespace)
        controller_factory.createController(V1Secret, {namespace}, self.SecretReconciler())

    class SecretReconciler(ResourceReconciler[V1Secret]):
        def reconcile(self, request: Request, lister: OperatorResourceLister[V1Secret]) -> Result:
            secret_opt = lister.get(request)
            LOG.info("Reconciling secret: %s", request)
            # .. reconcile
            return Result(False)
# end::class[]
