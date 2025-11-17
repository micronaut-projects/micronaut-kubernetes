package io.micronaut.kubernetes.client.openapi.operator.util

import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getConfigMapModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.getConfigMap

class ConfigMapOperation {

    private final CoreV1Api api

    ConfigMapOperation(CoreV1Api api) {
        this.api = api
    }

    V1ConfigMap createConfigMap(String name, String namespace, Map<String, String> data, Map<String, String> labels) {
        return createConfigMap(api, namespace, getConfigMapModel(name, data, labels))
    }

    String getProcessedAnnotation(String name, String namespace) {
        V1ConfigMap configMap = getConfigMap(api, name, namespace)
        Map<String, String> annotations = configMap.getMetadata().getAnnotations()
        return annotations == null ? null : annotations.get("io.micronaut.operator")
    }

    void removeProcessedAnnotation(String name, String namespace) {
        V1ConfigMap configMap = getConfigMap(api, name, namespace)
        Map<String, String> annotations = configMap.getMetadata().getAnnotations()
        if (annotations != null && annotations.containsKey("io.micronaut.operator")) {
            annotations.remove("io.micronaut.operator")
            api.replaceNamespacedConfigMap(name, namespace, configMap, null, null, null, null)
        }
    }
}
