package micronaut.operator;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api;
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer;
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import io.micronaut.kubernetes.client.openapi.operator.Operator;
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister;
import io.micronaut.kubernetes.client.openapi.operator.ResourceReconciler;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Requires(env = Environment.KUBERNETES)
@Operator(informer = @Informer(apiType = V1ConfigMap.class, namespace = ConfigMapReconciler.NAMESPACE))
class ConfigMapReconciler implements ResourceReconciler<V1ConfigMap> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigMapReconciler.class);

    static final String NAMESPACE = "test-operator-namespace";

    private final CoreV1Api coreV1Api;

    ConfigMapReconciler(CoreV1Api coreV1Api) {
        this.coreV1Api = coreV1Api;
    }

    @Override
    public Result reconcile(@NonNull Request request, @NonNull OperatorResourceLister<V1ConfigMap> lister) {
        LOG.info("Reconciling {}", request);
        Optional<V1ConfigMap> configMapOpt = lister.get(request);
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
        return new Result(false);
    }
}
