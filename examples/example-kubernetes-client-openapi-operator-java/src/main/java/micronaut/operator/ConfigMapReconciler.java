//tag::reconciler[]
package micronaut.operator;

import org.jspecify.annotations.NonNull;
//end::reconciler[]
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api;
//tag::reconciler[]
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
//end::reconciler[]
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
//tag::reconciler[]
import io.micronaut.kubernetes.client.openapi.operator.Operator;
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result;
//end::reconciler[]
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
//tag::reconciler[]
import java.util.Optional;

@Operator(informer = @Informer(apiType = V1ConfigMap.class)) // <1>
class ConfigMapReconciler implements ResourceReconciler<V1ConfigMap> { // <2>

    //end::reconciler[]

    private static final Logger LOG = LoggerFactory.getLogger(ConfigMapReconciler.class);

    private final CoreV1Api coreV1Api;

    ConfigMapReconciler(CoreV1Api coreV1Api) {
        this.coreV1Api = coreV1Api;
    }

    //tag::reconciler[]
    @Override
    @NonNull
    public Result reconcile(@NonNull Request request, @NonNull OperatorResourceLister<V1ConfigMap> lister) { // <3>
        Optional<V1ConfigMap> configMapOpt = lister.get(request); // <4>
        // .. reconcile  <5>
        //end::reconciler[]
        LOG.info("Reconciling config map: {}", request);
        if (configMapOpt.isPresent()) {
            V1ConfigMap configMap = configMapOpt.get();
            V1ObjectMeta metadata = configMap.getMetadata();

            Map<String, String> annotations = metadata.getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
                metadata.setAnnotations(annotations);
            }

            if (!annotations.containsKey("io.micronaut.operator")) {
                annotations.put("io.micronaut.operator", "processed");
                String name = configMap.getMetadata().getName();
                String namespace = configMap.getMetadata().getNamespace();
                try {
                    coreV1Api.replaceNamespacedConfigMap(name, namespace, configMap, null, null, null, null);
                } catch (Exception e) {
                    LOG.error("Failed to update config map", e);
                    return new Result(true, Duration.ofSeconds(2));
                }
            }
        }
        //tag::reconciler[]
        return new Result(false); // <6>
    }
}
//end::reconciler[]
