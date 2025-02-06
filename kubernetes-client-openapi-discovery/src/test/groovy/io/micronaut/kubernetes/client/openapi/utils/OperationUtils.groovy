package io.micronaut.kubernetes.client.openapi.utils

import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Endpoints
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
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
}
