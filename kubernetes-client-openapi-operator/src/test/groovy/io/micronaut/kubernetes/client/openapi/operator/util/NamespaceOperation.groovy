package io.micronaut.kubernetes.client.openapi.operator.util

import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta

class NamespaceOperation {

    private final CoreV1Api api

    NamespaceOperation(CoreV1Api api) {
        this.api = api
    }

    V1Namespace createNamespace(String namespaceName) {
        V1Namespace namespace = new V1Namespace()
        namespace.kind('Namespace')
        namespace.apiVersion('v1')
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(namespaceName)
        namespace.metadata(objectMeta)
        return api.createNamespace(namespace, null, null, null, null)
    }
}
