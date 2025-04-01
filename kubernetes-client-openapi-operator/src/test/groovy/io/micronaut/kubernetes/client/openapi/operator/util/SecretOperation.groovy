package io.micronaut.kubernetes.client.openapi.operator.util

import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Secret

class SecretOperation {

    private final CoreV1Api api

    SecretOperation(CoreV1Api api) {
        this.api = api
    }

    V1Secret getSecret(String name, String namespace) {
        return api.readNamespacedSecret(name, namespace, null)
    }

    V1Secret createSecret(String name, String namespace, Map<String, String> labels) {
        V1Secret secret = new V1Secret()
        secret.kind('Secret')
        secret.apiVersion('v1')
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(name)
        objectMeta.labels(labels)
        secret.metadata(objectMeta)
        secret.data(["test-key": "test-value".bytes])
        return api.createNamespacedSecret(namespace, secret, null, null, null, null)
    }

    String getProcessedAnnotation(String name, String namespace) {
        V1Secret secret = getSecret(name, namespace)
        Map<String, String> annotations = secret.getMetadata().getAnnotations()
        return annotations == null ? null : annotations.get("io.micronaut.operator")
    }

    void removeProcessedAnnotation(String name, String namespace) {
        V1Secret secret = getSecret(name, namespace)
        Map<String, String> annotations = secret.getMetadata().getAnnotations()
        if (annotations != null && annotations.containsKey("io.micronaut.operator")) {
            annotations.remove("io.micronaut.operator")
            api.replaceNamespacedSecret(name, namespace, secret, null, null, null, null)
        }
    }
}
