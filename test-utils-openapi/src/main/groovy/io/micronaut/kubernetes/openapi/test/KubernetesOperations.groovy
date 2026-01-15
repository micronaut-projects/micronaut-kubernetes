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

import io.micronaut.kubernetes.client.openapi.api.AppsV1Api
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.api.RbacAuthorizationV1Api
import io.micronaut.kubernetes.client.openapi.api.ResourceV1Api
import io.micronaut.kubernetes.client.openapi.api.VersionApi
import io.micronaut.kubernetes.client.openapi.model.ResourceV1ResourceClaim
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Deployment
import io.micronaut.kubernetes.client.openapi.model.V1Endpoints
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1NamespaceList
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1PodList
import io.micronaut.kubernetes.client.openapi.model.V1Role
import io.micronaut.kubernetes.client.openapi.model.V1RoleBinding
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.model.V1Service
import io.micronaut.kubernetes.client.openapi.model.VersionInfo
import io.micronaut.kubernetes.client.openapi.response.DeleteResponse
import org.awaitility.Awaitility
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.stream.Collectors

class KubernetesOperations {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesOperations.class)

    static VersionInfo getVersionInfo(VersionApi api) {
        return api.getCode()
    }

    static V1Namespace createNamespace(CoreV1Api api, V1Namespace namespace) {
        api.createNamespace(namespace, null, null, null, null)
    }

    static V1NamespaceList listNamespace(CoreV1Api api) {
        api.listNamespace(null, null, null, null, null, null, null, null, null, null, null)
    }

    static DeleteResponse<V1Namespace> deleteNamespace(CoreV1Api api, String name) {
        api.deleteNamespace(name, null, null, null, null, null, null, null)
    }

    static void deleteNamespaceSafe(CoreV1Api api, String name) {
        LOG.debug("Deleting namespace: ${name}")
        deleteNamespace(api, name)
        Awaitility.await()
                .pollDelay(Duration.ofMillis(100))
                .pollInterval(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(60))
                .until(() -> {
                    V1NamespaceList namespaceList = listNamespace(api)
                    Set<String> namespaces = namespaceList.items.stream().map(it -> it.metadata.name).collect(Collectors.toSet())
                    if (namespaces.contains(name)) {
                        LOG.debug("Namespace ${namespaces} still exists, trying again in 3s")
                        return false
                    } else {
                        LOG.debug("Namespace sucessfully deleted, returned namespaces: ${namespaces}")
                        return true
                    }
                })
    }

    static V1Endpoints createEndpoints(CoreV1Api api, String namespace, V1Endpoints endpoints) {
        api.createNamespacedEndpoints(namespace, endpoints, null, null, null, null)
    }

    static V1Endpoints getEndpoints(CoreV1Api api, String name, String namespace) {
        api.readNamespacedEndpoints(name, namespace, null)
    }

    static V1Service createService(CoreV1Api api, String namespace, V1Service service) {
        api.createNamespacedService(namespace, service, null, null, null, null)
    }

    static V1ConfigMap createConfigMap(CoreV1Api api, String namespace, V1ConfigMap configMap) {
        api.createNamespacedConfigMap(namespace, configMap, null, null, null, null)
    }

    static V1ConfigMap getConfigMap(CoreV1Api api, String name, String namespace) {
        api.readNamespacedConfigMap(name, namespace, null)
    }

    static V1ConfigMap replaceConfigMap(CoreV1Api api, String namespace, V1ConfigMap configMap) {
        api.replaceNamespacedConfigMap(configMap.getMetadata().getName(), namespace, configMap, null, null, null, null)
    }

    static DeleteResponse<V1ConfigMap> deleteConfigMap(CoreV1Api api, String namespace, String name) {
        api.deleteNamespacedConfigMap(name, namespace, null, null, null, null, null, null, null)
    }

    static V1Pod createPod(CoreV1Api api, String namespace, V1Pod pod) {
        api.createNamespacedPod(namespace, pod, null, null, null, null)
    }

    static V1PodList listPodForAllNamespaces(CoreV1Api api) {
        api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null)
    }

    static V1PodList listPod(CoreV1Api api, String namespace, String labelSelector = null) {
        api.listNamespacedPod(namespace, null, null, null, null, labelSelector, null, null, null, null, null, null)
    }

    static V1Role createRole(RbacAuthorizationV1Api api, String namespace, V1Role role) {
        api.createNamespacedRole(namespace, role, null, null, null, null)
    }

    static V1RoleBinding createRoleBinding(RbacAuthorizationV1Api api, String namespace, V1RoleBinding roleBinding) {
        api.createNamespacedRoleBinding(namespace, roleBinding, null, null, null, null)
    }

    static V1Secret createSecret(CoreV1Api api, String namespace, V1Secret secret) {
        api.createNamespacedSecret(namespace, secret, null, null, null, null)
    }

    static V1Secret getSecret(CoreV1Api api, String name, String namespace) {
        api.readNamespacedSecret(name, namespace, null)
    }

    static V1Secret replaceSecret(CoreV1Api api, String namespace, V1Secret secret) {
        api.replaceNamespacedSecret(secret.getMetadata().getName(), namespace, secret, null, null, null, null)
    }

    static DeleteResponse<V1Secret> deleteSecret(CoreV1Api api, String namespace, String name) {
        api.deleteNamespacedSecret(name, namespace, null, null, null, null, null, null, null)
    }

    static V1Service getService(CoreV1Api api, String name, String namespace) {
        api.readNamespacedService(name, namespace, null)
    }

    static V1Deployment createDeployment(AppsV1Api api, String namespace, V1Deployment deployment) {
        api.createNamespacedDeployment(namespace, deployment, null, null, null, null)
    }

    static V1Deployment getDeployment(AppsV1Api api, String name, String namespace) {
        api.readNamespacedDeployment(name, namespace, null)
    }

    static void createResourceClaim(ResourceV1Api api, String namespace, ResourceV1ResourceClaim resourceClaim) {
        api.createNamespacedResourceClaim(namespace, resourceClaim, null, null, null, null)
    }

    static DeleteResponse<ResourceV1ResourceClaim> deleteResourceClaim(ResourceV1Api api, String namespace, String name) {
        api.deleteNamespacedResourceClaim(name, namespace, null, null, null, null, null, null, null)
    }

    static KubectlPortForward portForwardService(CoreV1Api api, String serviceName, String namespace, int port, int localPort) {
        LOG.debug("Forwarding service ${namespace}/${serviceName} port ${port} to ${localPort}")

        V1Service service = getService(api, serviceName, namespace)

        service.spec.ports.stream()
                .filter(s -> s.port == port)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Service ${namespace}/${name} doesn't contain port ${port}"))

        List<String> labelSelectors = []
        service.spec.selector.each { key, val ->
            labelSelectors.add("$key=$val")
        }

        V1PodList podList = listPod(api, namespace, labelSelectors.join(","))
        V1Pod pod = podList.items.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find matching pod for service:" + service))

        return new KubectlPortForward(namespace, pod.metadata.name, port, localPort)
    }
}
