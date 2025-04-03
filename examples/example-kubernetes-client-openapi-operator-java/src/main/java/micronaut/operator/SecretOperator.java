package micronaut.operator;

import io.micronaut.context.annotation.Context;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister;
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerFactory;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;

@Context
class SecretOperator {

    private static final Logger LOG = LoggerFactory.getLogger(SecretOperator.class);

    static final String NAMESPACE = "test-operator-namespace";

    SecretOperator(SharedIndexInformerFactory sharedIndexInformerFactory, ControllerFactory controllerFactory) {
        sharedIndexInformerFactory.sharedIndexInformerFor(V1Secret.class, NAMESPACE);
        controllerFactory.createController(V1Secret.class, Collections.singleton(NAMESPACE), new SecretReconciler());
    }

    private class SecretReconciler implements ResourceReconciler<V1Secret> {
        @Override
        public Result reconcile(Request request, OperatorResourceLister<V1Secret> lister) {
            Optional<V1Secret> secretOpt = lister.get(request);
            LOG.info("Reconciling secret: {}", request);
            // .. reconcile
            return new Result(false);
        }
    }
}
