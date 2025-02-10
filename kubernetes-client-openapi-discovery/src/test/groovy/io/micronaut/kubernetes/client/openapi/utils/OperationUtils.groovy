package io.micronaut.kubernetes.client.openapi.utils

import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Endpoints
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.model.V1Service
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor

class OperationUtils {

    static V1Namespace createNamespace(CoreV1ApiReactor api, V1Namespace namespace) {
        api.createNamespace(namespace, null, null, null, null).block()
    }

    static V1Endpoints createEndpoints(CoreV1ApiReactor api, String namespace, V1Endpoints endpoints) {
        api.createNamespacedEndpoints(namespace, endpoints, null, null, null, null).block()
    }

    static V1Service createService(CoreV1ApiReactor api, String namespace, V1Service service) {
        api.createNamespacedService(namespace, service, null, null, null, null).block()
    }

    static V1ConfigMap createConfigMap(CoreV1ApiReactor api, String namespace, V1ConfigMap configMap) {
        api.createNamespacedConfigMap(namespace, configMap, null, null, null, null).block()
    }

    static V1ConfigMap replaceConfigMap(CoreV1ApiReactor api, String namespace, V1ConfigMap configMap) {
        api.replaceNamespacedConfigMap(configMap.getMetadata().getName(), namespace, configMap, null, null, null, null).block()
    }

    static void deleteConfigMap(CoreV1ApiReactor api, String namespace, String name) {
        api.deleteNamespacedConfigMap(name, namespace, null, null, null, null, null, null).block()
    }

    static V1Pod createPod(CoreV1ApiReactor api, String namespace, V1Pod pod) {
        api.createNamespacedPod(namespace, pod, null, null, null, null).block()
    }

    static V1Secret createSecret(CoreV1ApiReactor api, String namespace, V1Secret secret) {
        api.createNamespacedSecret(namespace, secret, null, null, null, null).block()
    }

    static V1Secret replaceSecret(CoreV1ApiReactor api, String namespace, V1Secret secret) {
        api.replaceNamespacedSecret(secret.getMetadata().getName(), namespace, secret, null, null, null, null).block()
    }

    static void deleteSecret(CoreV1ApiReactor api, String namespace, String name) {
        api.deleteNamespacedSecret(name, namespace, null, null, null, null, null, null).block()
    }
}
