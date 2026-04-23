/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.kubernetes.test

import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api
import io.kubernetes.client.openapi.apis.VersionApi
import io.kubernetes.client.openapi.models.RbacV1Subject
import io.kubernetes.client.openapi.models.V1ClusterRole
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1Endpoints
import io.kubernetes.client.openapi.models.V1EndpointsList
import io.kubernetes.client.openapi.models.V1Namespace
import io.kubernetes.client.openapi.models.V1NamespaceList
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodList
import io.kubernetes.client.openapi.models.V1PolicyRule
import io.kubernetes.client.openapi.models.V1Role
import io.kubernetes.client.openapi.models.V1RoleBinding
import io.kubernetes.client.openapi.models.V1RoleRef
import io.kubernetes.client.openapi.models.V1Secret
import io.kubernetes.client.openapi.models.V1SecretList
import io.kubernetes.client.openapi.models.V1Service
import io.kubernetes.client.openapi.models.V1ServiceList
import io.kubernetes.client.openapi.models.V1ServiceSpec
import io.kubernetes.client.openapi.models.VersionInfo
import io.kubernetes.client.util.Yaml
import io.kubernetes.client.util.wait.Wait
import io.micronaut.core.util.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.stream.Collectors

import static io.micronaut.kubernetes.test.KubernetesModels.getClusterRoleModel
import static io.micronaut.kubernetes.test.KubernetesModels.getConfigMapModel
import static io.micronaut.kubernetes.test.KubernetesModels.getEndpointsModel
import static io.micronaut.kubernetes.test.KubernetesModels.getPolicyRuleModel
import static io.micronaut.kubernetes.test.KubernetesModels.getRoleBindingModel
import static io.micronaut.kubernetes.test.KubernetesModels.getRoleModel
import static io.micronaut.kubernetes.test.KubernetesModels.getRoleRefModel
import static io.micronaut.kubernetes.test.KubernetesModels.getSecretModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServiceModel
import static io.micronaut.kubernetes.test.KubernetesModels.getSubjectModel

/**
 * Kubernetes operations using the official java client.
 */
class KubernetesOperations {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesOperations.class)

    static VersionInfo getVersionInfo() {
        return new VersionApi().getCode().execute()
    }

    static V1Namespace createNamespace(String name) {
        LOG.debug("Create namespace $name")
        return new CoreV1Api().createNamespace(KubernetesModels.getNamespaceModel(name)).execute()
    }

    static V1Namespace getNamespace(String name) {
        return new CoreV1Api().readNamespace(name).execute()
    }

    static void deleteNamespace(String name) {
        LOG.debug("Deleting namespace ${name}")
        CoreV1Api coreV1Api = new CoreV1Api()
        coreV1Api.deleteNamespace(name).execute()

        Wait.poll(
            Duration.ofMillis(100),
            Duration.ofSeconds(3),
            Duration.ofSeconds(60),
            () -> {
                V1NamespaceList namespaceList = coreV1Api.listNamespace().execute()
                Set<String> namespaces = namespaceList.items.stream().map(it -> it.metadata.name).collect(Collectors.toSet())
                if (namespaces.contains(name)) {
                    LOG.debug("Namespace ${namespaces} still exists, trying again in 500ms")
                    return false
                } else {
                    LOG.debug("Namespace sucessfully deleted, returned namespaces: ${namespaces}")
                    return true
                }
            })
    }

    static V1Role createRole(String name,
                             String namespace,
                             List<String> apiGroups = [""],
                             List<String> verbs = ["get", "list", "watch"],
                             List<String> resources = ["services", "endpoints", "configmaps", "secrets", "pods"]) {
        V1PolicyRule policyRule = getPolicyRuleModel(apiGroups, verbs, resources)
        V1Role role = getRoleModel(name, [policyRule])

        LOG.debug("Creating Role ${role}")

        RbacAuthorizationV1Api rbacAuthV1Api = new RbacAuthorizationV1Api()
        return rbacAuthV1Api.createNamespacedRole(namespace, role).execute()
    }

    static V1RoleBinding createRoleBinding(String name,
                                    String namespace,
                                    String roleRefName,
                                    String accountName = "default") {
        RbacV1Subject subject = getSubjectModel("ServiceAccount", accountName, namespace)
        V1RoleRef roleRef = getRoleRefModel(roleRefName)
        V1RoleBinding roleBinding = getRoleBindingModel(name, roleRef, [subject])

        LOG.debug("Creating Role Binding ${roleBinding}")

        RbacAuthorizationV1Api rbacAuthV1Api = new RbacAuthorizationV1Api()
        return rbacAuthV1Api.createNamespacedRoleBinding(namespace, roleBinding).execute()
    }

    static V1ConfigMap getConfigMap(String name, String namespace) {
        return new CoreV1Api().readNamespacedConfigMap(name, namespace).execute()
    }

    static V1ConfigMap getConfigMapNotFoundSafe(String name, String namespace) {
        try {
            return getConfigMap(name, namespace)
        } catch (ApiException e) {
            if (e.code != 404) {
                throw e
            }
            return null
        }
    }

    static V1ConfigMap replaceConfigMap(String namespace, V1ConfigMap configMap) {
        return new CoreV1Api().replaceNamespacedConfigMap(configMap.getMetadata().getName(), namespace, configMap).execute()
    }

    static V1ConfigMap createConfigMap(String name,
                                String namespace,
                                Map<String, String> data = ['foo': 'bar'],
                                Map<String, String> labels = [:],
                                Map<String, String> annotations = [:]) {

        V1ConfigMap configMap = getConfigMapModel(name, data, labels, annotations)
        return createConfigMap(namespace, configMap)
    }

    static V1ConfigMap createConfigMap(String namespace, V1ConfigMap configMap) {
        LOG.debug("Creating ConfigMap ${configMap}")
        CoreV1Api coreV1Api = new CoreV1Api()
        return coreV1Api.createNamespacedConfigMap(namespace, configMap).execute()
    }

    static V1ConfigMap createConfigMapFromFile(String name,
                                        String namespace,
                                        URL path,
                                        Map<String, String> labels = [:]) {

        Map<String, String> data = [:]
        data.put(new File(path.toURI().toString()).name, path.text)
        V1ConfigMap configMap = getConfigMapModel(name, data, labels)

        LOG.debug("Creating ConfigMap ${configMap}")

        CoreV1Api coreV1Api = new CoreV1Api()
        return coreV1Api.createNamespacedConfigMap(namespace, configMap).execute()
    }

    static void deleteConfigMap(String name, String namespace) {
        LOG.debug("Deleting config map ${namespace}/${name}")
        CoreV1Api coreV1Api = new CoreV1Api()
        coreV1Api.deleteNamespacedConfigMap(name, namespace).execute()
    }

    static void deleteConfigMapNotFoundSafe(String name, String namespace) {
        try {
            deleteConfigMap(name, namespace)
        } catch (ApiException e) {
            if (e.code != 404) {
                throw e
            }
            // ignore
        }
    }

    static V1ConfigMap modifyConfigMap(V1ConfigMap configMap) {
        LOG.debug("Modifying config map ${configMap}")
        CoreV1Api coreV1Api = new CoreV1Api()
        return coreV1Api.replaceNamespacedConfigMap(configMap.metadata.name, configMap.metadata.namespace, configMap).execute()
    }

    static V1ConfigMap modifyConfigMap(String name, String namespace, Map data = [foo: 'baz']) {
        V1ConfigMap configMap = getConfigMapModel(name, data)
        return new CoreV1Api().replaceNamespacedConfigMap(name, namespace, configMap).execute()
    }

    static V1ConfigMapList listConfigMaps(String namespace) {
        return new CoreV1Api().listNamespacedConfigMap(namespace).execute()
    }

    static V1Secret createSecret(String name, String namespace, Map<String, byte[]> data, Map<String, String> labels = [:]) {
        V1Secret secret = getSecretModel(name, data, labels)
        return createSecret(namespace, secret)
    }

    static V1Secret createSecret(String namespace, V1Secret secret) {
        LOG.debug("Creating ${secret}")
        return new CoreV1Api().createNamespacedSecret(namespace, secret).execute()
    }

    static V1Secret getSecret(String name, String namespace) {
        return new CoreV1Api().readNamespacedSecret(name, namespace).execute()
    }

    static V1SecretList listSecrets(String namespace) {
        return new CoreV1Api().listNamespacedSecret(namespace).execute()
    }

    static V1Secret replaceSecret(String namespace, V1Secret secret) {
        return new CoreV1Api().replaceNamespacedSecret(secret.getMetadata().getName(), namespace, secret).execute()
    }

    static void deleteSecret(String name, String namespace) {
        new CoreV1Api().deleteNamespacedSecret(name, namespace).execute()
    }

    static V1Deployment createDeployment(String namespace, V1Deployment deployment) {
        LOG.debug("Creating ${deployment}")

        new AppsV1Api().createNamespacedDeployment(namespace, deployment).execute()

        LOG.debug("Waiting until deployment is ready or timeout of 120s expires")

        V1Deployment createdDeployment = null
        Wait.poll(
                Duration.ofSeconds(3),
                Duration.ofSeconds(180),
                () -> {
                    createdDeployment = getDeployment(deployment.metadata.name, namespace)
                    return createdDeployment.getStatus().getReadyReplicas() > 0
                })
        return createdDeployment
    }

    static V1Deployment createDeploymentFromFile(URL pathToManifest, String name = null, String namespace = null) {
        String content = pathToManifest.text
        V1Deployment deployment = (V1Deployment) Yaml.load(content)

        if (StringUtils.isNotEmpty(name)) {
            deployment.metadata.name = name
        }

        if (StringUtils.isNotEmpty(namespace)) {
            deployment.metadata.namespace = namespace
        }

        LOG.debug("Creating ${deployment}")

        new AppsV1Api().createNamespacedDeployment(deployment.metadata.namespace, deployment).execute()

        LOG.debug("Waiting until deployment is ready or timeout of 120s expires")

        V1Deployment createdDeployment = null
        Wait.poll(
            Duration.ofSeconds(3),
            Duration.ofSeconds(180),
            () -> {
                createdDeployment = getDeployment(deployment.metadata.name, deployment.metadata.namespace)
                return createdDeployment.getStatus().getReadyReplicas() > 0
            })
        return createdDeployment
    }

    static V1Deployment getDeployment(String name, String namespace) {
        return new AppsV1Api().readNamespacedDeployment(name, namespace).execute()
    }

    static V1Service createService(String name, String namespace, V1ServiceSpec serviceSpec, Map<String, String> labels = [:]) {
        V1Service service = getServiceModel(name, serviceSpec, labels)

        LOG.debug("Creating ${service}")

        V1Service createdService = new CoreV1Api().createNamespacedService(namespace, service).execute()

        // in case of headless service or ExternalName service do now wait
        if (!(createdService.spec.externalName)) {
            LOG.debug("Polling for Endpoints get ready ${createdService}")
            Wait.poll(
                Duration.ofSeconds(2),
                Duration.ofSeconds(20),
                () -> getEndpoints(name, namespace) != null)
        }
        return createdService
    }

    static V1Service getService(String name, String namespace) {
        return new CoreV1Api().readNamespacedService(name, namespace).execute()
    }

    static V1ServiceList listServices(String namespace) {
        return new CoreV1Api().listNamespacedService(namespace).execute()
    }

    static void deleteService(String name, String namespace) {
        try {
            new CoreV1Api().deleteNamespacedService(name, namespace).execute()
        } catch (IllegalArgumentException ignored) {
            // ignore since this is a known kubernetes java client issue which happens
            // when the kubernetes api service returns V1Status instead of V1Service
        }
    }

    static V1Endpoints createEndpoints(String name, String namespace) {
        V1Endpoints endpoints = getEndpointsModel(name)

        LOG.debug("Creating Endpoints ${endpoints}")

        CoreV1Api coreV1Api = new CoreV1Api()
        return coreV1Api.createNamespacedEndpoints(namespace, endpoints).execute()
    }

    static V1Endpoints getEndpoints(String name, String namespace) {
        return new CoreV1Api().readNamespacedEndpoints(name, namespace).execute()
    }

    static V1EndpointsList listEndpoints(String namespace) {
        return new CoreV1Api().listNamespacedEndpoints(namespace).execute()
    }

    static void deleteEndpoints(String name, String namespace) {
        new CoreV1Api().deleteNamespacedEndpoints(name, namespace).execute()
    }

    static V1Pod createPod(String namespace, V1Pod pod) {
        return new CoreV1Api().createNamespacedPod(namespace, pod).execute()
    }

    static V1PodList listPods(String namespace, String labelSelector = null) {
        return new CoreV1Api().listNamespacedPod(namespace).labelSelector(labelSelector).execute()
    }

    static V1ClusterRole createClusterRole(String name,
                                           List<String> apiGroups = ["*"],
                                           List<String> verbs = ["get"],
                                           List<String> resources = ["*"]) {
        V1PolicyRule policyRule = getPolicyRuleModel(apiGroups, verbs, resources)
        V1ClusterRole clusterRole = getClusterRoleModel(name, [policyRule])

        LOG.debug("Creating Cluster Role ${clusterRole}")

        return new RbacAuthorizationV1Api().createClusterRole(clusterRole).execute()
    }

    static V1ClusterRole modifyClusterRole(V1ClusterRole clusterRole) {
        LOG.debug("Modifying cluster role ${clusterRole}")
        return new RbacAuthorizationV1Api().replaceClusterRole(clusterRole.metadata.name, clusterRole).execute()
    }

    static void deleteClusterRole(String name) {
        LOG.debug("Deleting cluster role ${name}")
        new RbacAuthorizationV1Api().deleteClusterRole(name).execute()
    }

    static KubectlPortForward portForwardService(String serviceName, String namespace, int port, int localPort) {
        LOG.debug("Forwarding service ${namespace}/${serviceName} port ${port} to ${localPort}")

        V1Service service = getService(serviceName, namespace)

        service.spec.ports.stream()
            .filter(s -> s.port == port)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Service ${namespace}/${name} doesn't contain port ${port}"))

        List<String> labelSelectors = []
        service.spec.selector.each { key, val ->
            labelSelectors.add("$key=$val")
        }

        V1PodList podList = listPods(namespace, labelSelectors.join(","))
        V1Pod pod = podList.items.stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Could not find matching pod for service:" + service))

        return new KubectlPortForward(namespace, pod.metadata.name, port, localPort)
    }
}
