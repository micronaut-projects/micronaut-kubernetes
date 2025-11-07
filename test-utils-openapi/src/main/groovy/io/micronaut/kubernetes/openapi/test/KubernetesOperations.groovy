/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.openapi.test

import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Endpoints
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.model.V1Service

class KubernetesOperations {

    static V1Namespace createNamespace(CoreV1Api api, V1Namespace namespace) {
        api.createNamespace(namespace, null, null, null, null)
    }

    static V1Endpoints createEndpoints(CoreV1Api api, String namespace, V1Endpoints endpoints) {
        api.createNamespacedEndpoints(namespace, endpoints, null, null, null, null)
    }

    static V1Service createService(CoreV1Api api, String namespace, V1Service service) {
        api.createNamespacedService(namespace, service, null, null, null, null)
    }

    static V1ConfigMap createConfigMap(CoreV1Api api, String namespace, V1ConfigMap configMap) {
        api.createNamespacedConfigMap(namespace, configMap, null, null, null, null)
    }

    static V1ConfigMap replaceConfigMap(CoreV1Api api, String namespace, V1ConfigMap configMap) {
        api.replaceNamespacedConfigMap(configMap.getMetadata().getName(), namespace, configMap, null, null, null, null)
    }

    static void deleteConfigMap(CoreV1Api api, String namespace, String name) {
        api.deleteNamespacedConfigMap(name, namespace, null, null, null, null, null, null, null)
    }

    static V1Pod createPod(CoreV1Api api, String namespace, V1Pod pod) {
        api.createNamespacedPod(namespace, pod, null, null, null, null)
    }

    static V1Secret createSecret(CoreV1Api api, String namespace, V1Secret secret) {
        api.createNamespacedSecret(namespace, secret, null, null, null, null)
    }

    static V1Secret replaceSecret(CoreV1Api api, String namespace, V1Secret secret) {
        api.replaceNamespacedSecret(secret.getMetadata().getName(), namespace, secret, null, null, null, null)
    }

    static void deleteSecret(CoreV1Api api, String namespace, String name) {
        api.deleteNamespacedSecret(name, namespace, null, null, null, null, null, null, null)
    }
}
