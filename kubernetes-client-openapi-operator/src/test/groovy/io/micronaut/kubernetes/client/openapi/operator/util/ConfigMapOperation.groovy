package io.micronaut.kubernetes.client.openapi.operator.util

import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta

class ConfigMapOperation {

    private final CoreV1Api api

    ConfigMapOperation(CoreV1Api api) {
        this.api = api
    }

    V1ConfigMap getConfigMap(String name, String namespace) {
        return api.readNamespacedConfigMap(name, namespace, null)
    }

    V1ConfigMap createConfigMap(String name, String namespace, Map<String, String> data, Map<String, String> labels) {
        V1ConfigMap configMap = new V1ConfigMap()
        configMap.kind('ConfigMap')
        configMap.apiVersion('v1')
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(name)
        objectMeta.labels(labels)
        configMap.metadata(objectMeta)
        configMap.data(data)
        return api.createNamespacedConfigMap(namespace, configMap, null, null, null, null)
    }

    String getProcessedAnnotation(String name, String namespace) {
        V1ConfigMap configMap = getConfigMap(name, namespace)
        Map<String, String> annotations = configMap.getMetadata().getAnnotations()
        return annotations == null ? null : annotations.get("io.micronaut.operator")
    }

    void removeProcessedAnnotation(String name, String namespace) {
        V1ConfigMap configMap = getConfigMap(name, namespace)
        Map<String, String> annotations = configMap.getMetadata().getAnnotations()
        if (annotations != null && annotations.containsKey("io.micronaut.operator")) {
            annotations.remove("io.micronaut.operator")
            api.replaceNamespacedConfigMap(name, namespace, configMap, null, null, null, null)
        }
    }
}
