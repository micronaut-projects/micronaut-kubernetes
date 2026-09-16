//tag::class[]
package micronaut.operator

import io.micronaut.context.annotation.Context
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerFactory
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver
import org.slf4j.LoggerFactory
//end::class[]
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment

@Requires(env = [Environment.KUBERNETES])
//tag::class[]

@Context
class SecretOperator(sharedIndexInformerFactory: SharedIndexInformerFactory, controllerFactory: ControllerFactory, namespaceResolver: NamespaceResolver) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SecretOperator::class.java)
    }

    init {
        val namespace = namespaceResolver.resolveNamespace()
        sharedIndexInformerFactory.sharedIndexInformerFor(V1Secret::class.java, namespace)
        controllerFactory.createController(V1Secret::class.java, setOf(namespace), SecretReconciler())
    }

    private inner class SecretReconciler : ResourceReconciler<V1Secret> {
        override fun reconcile(request: Request, lister: OperatorResourceLister<V1Secret>): Result {
            val secretOpt = lister.get(request)
            LOG.info("Reconciling secret: {}", request)
            // .. reconcile
            return Result(false)
        }
    }
}
//end::class[]
