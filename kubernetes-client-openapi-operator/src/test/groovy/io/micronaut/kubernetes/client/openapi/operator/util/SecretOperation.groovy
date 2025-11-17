package io.micronaut.kubernetes.client.openapi.operator.util

import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1Secret

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getSecretModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.getSecret

class SecretOperation {

    private final CoreV1Api api

    SecretOperation(CoreV1Api api) {
        this.api = api
    }

    V1Secret createSecret(String name, String namespace, Map<String, String> labels) {
        V1Secret secret = getSecretModel(name, ["test-key": "test-value".bytes], labels)
        return createSecret(api, namespace, secret)
    }

    String getProcessedAnnotation(String name, String namespace) {
        V1Secret secret = getSecret(api, name, namespace)
        Map<String, String> annotations = secret.getMetadata().getAnnotations()
        return annotations == null ? null : annotations.get("io.micronaut.operator")
    }

    void removeProcessedAnnotation(String name, String namespace) {
        V1Secret secret = getSecret(api, name, namespace)
        Map<String, String> annotations = secret.getMetadata().getAnnotations()
        if (annotations != null && annotations.containsKey("io.micronaut.operator")) {
            annotations.remove("io.micronaut.operator")
            api.replaceNamespacedSecret(name, namespace, secret, null, null, null, null)
        }
    }
}
