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

import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.core.io.ResourceResolver
import io.micronaut.kubernetes.client.openapi.api.AppsV1Api
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.api.RbacAuthorizationV1Api
import io.micronaut.kubernetes.client.openapi.api.VersionApi
import io.micronaut.kubernetes.client.openapi.config.AbstractKubeConfigLoader
import io.micronaut.kubernetes.client.openapi.config.DefaultKubeConfigLoader
import io.micronaut.kubernetes.client.openapi.config.KubeConfig
import io.micronaut.kubernetes.client.openapi.model.RbacV1Subject
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1Container
import io.micronaut.kubernetes.client.openapi.model.V1ContainerPort
import io.micronaut.kubernetes.client.openapi.model.V1Deployment
import io.micronaut.kubernetes.client.openapi.model.V1DeploymentSpec
import io.micronaut.kubernetes.client.openapi.model.V1LabelSelector
import io.micronaut.kubernetes.client.openapi.model.V1PodSpec
import io.micronaut.kubernetes.client.openapi.model.V1PodTemplateSpec
import io.micronaut.kubernetes.client.openapi.model.V1PolicyRule
import io.micronaut.kubernetes.client.openapi.model.V1Role
import io.micronaut.kubernetes.client.openapi.model.V1RoleBinding
import io.micronaut.kubernetes.client.openapi.model.V1RoleRef
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.model.V1Service
import io.micronaut.kubernetes.client.openapi.model.V1ServiceSpec
import io.micronaut.kubernetes.client.openapi.model.V1Volume
import io.micronaut.kubernetes.client.openapi.model.V1VolumeMount
import io.micronaut.kubernetes.client.openapi.model.VersionInfo
import io.micronaut.kubernetes.client.openapi.type.IntOrString
import io.micronaut.test.context.TestContext
import io.micronaut.test.context.TestExecutionListener
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.awaitility.Awaitility
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getConfigMapModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getConfigMapVolumeSourceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getHTTPGetActionModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getNamespaceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getObjectMetaModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getPolicyRuleModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getProbeModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getRoleBindingModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getRoleModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getRoleRefModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getSecretModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getSecretVolumeSourceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getServiceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getServicePortModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getServiceSpecTypeModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getSubjectModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getVolumeModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getVolumeMountModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createDeployment
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createRole
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createRoleBinding
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createService
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.deleteNamespaceSafe
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.getDeployment
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.getEndpoints
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.getVersionInfo

class KubernetesSpecification extends Specification {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesSpecification.class)

    @Inject
    @Shared
    @Value('${kubernetes.client.namespace:micronaut-kubernetes-openapi}')
    String namespace

    @Inject
    @Shared
    VersionApi versionApi

    @Inject
    @Shared
    CoreV1Api coreV1Api

    @Inject
    @Shared
    RbacAuthorizationV1Api rbacAuthV1Api

    @Inject
    @Shared
    AppsV1Api appsV1Api

    def setupSpec() {
        setupPrepare()
        VersionInfo versionInfo = getVersionInfo(versionApi)
        LOG.info("Using Kubernetes version: {}.{}", versionInfo.major, versionInfo.minor)
        createNamespaceSafe()
        createRole("example-openapi-role")
        createRoleBinding("example-openapi-role-binding", "example-openapi-role")
        createResources()
    }

    def setupPrepare() {
    }

    def createNamespaceSafe() {
        if (coreV1Api.readNamespace(namespace, null) != null) {
            deleteNamespaceSafe(coreV1Api, namespace)
        }
        LOG.debug("Creating namespace: {}", namespace)
        createNamespace(coreV1Api, getNamespaceModel(namespace))
    }

    def createRole(String name,
                   List<String> apiGroups = ["", "coordination.k8s.io"],
                   List<String> verbs = ["get", "create", "update", "list", "watch"],
                   List<String> resources = ["services", "endpoints", "configmaps", "secrets", "pods", "leases"]) {
        V1PolicyRule policyRule = getPolicyRuleModel(apiGroups, verbs, resources)
        V1Role role = getRoleModel(name, [policyRule])
        LOG.debug("Creating Role: {}", role)
        return createRole(rbacAuthV1Api, namespace, role)
    }

    def createRoleBinding(String name,
                          String roleRefName,
                          String accountName = "default") {
        RbacV1Subject subject = getSubjectModel("ServiceAccount", accountName, namespace)
        V1RoleRef roleRef = getRoleRefModel(roleRefName)
        V1RoleBinding roleBinding = getRoleBindingModel(name, roleRef, [subject])
        LOG.debug("Creating RoleBinding: {}", roleBinding)
        return createRoleBinding(rbacAuthV1Api, namespace, roleBinding)
    }

    def createResources() {
    }

    def createTestConfigMap() {
        def propertiesContent = '''\
            enemies=zombies
            lives=5
            enemies.cheat=true
            enemies.cheat.level=noGoodRotten\
        '''.stripIndent()
        V1ConfigMap configMap = getConfigMapModel("test-configmap", [ "game.properties": propertiesContent])
        LOG.debug("Creating ConfigMap: {}", configMap)
        createConfigMap(coreV1Api, namespace, configMap)
    }

    def createMountedConfigMap() {
        def content = '''\
            mounted:
              foo: bar\
        '''.stripIndent()
        V1ConfigMap configMap = getConfigMapModel("mounted-configmap", [ "mounted.yml": content])
        LOG.debug("Creating ConfigMap: {}", configMap)
        createConfigMap(coreV1Api, namespace, configMap)
    }

    def createTestSecret() {
        V1Secret secret = getSecretModel("test-secret", ["secretProperty": "secretValue".bytes])
        LOG.debug("Creating Secret: {}", secret)
        createSecret(coreV1Api, namespace, secret)
    }

    def createMountedSecret() {
        V1Secret secret = getSecretModel("mounted-secret", ["mountedVolumeKey": "mountedVolumeValue".bytes])
        LOG.debug("Creating Secret: {}", secret)
        createSecret(coreV1Api, namespace, secret)
    }

    def createMountedSecretProp() {
        V1Secret secret = getSecretModel("mounted-secret", ["mounted-secret.properties": "mounted-secret-key=mounted-secret-value".bytes])
        LOG.debug("Creating Secret: {}", secret)
        createSecret(coreV1Api, namespace, secret)
    }

    def createDeployment(String name, String image, int port, boolean includeVolume) {
        List<V1VolumeMount> volumeMounts = includeVolume
                ? [getVolumeMountModel("/etc/example-service/secrets", "secrets", true),
                   getVolumeMountModel("/etc/example-service/configmap", "configmap", true)]
                : null

        List<V1Volume> volumes = includeVolume
                ? [getVolumeModel("secrets", getSecretVolumeSourceModel("mounted-secret", 256)),
                   getVolumeModel("configmap", getConfigMapVolumeSourceModel("mounted-configmap"))]
                : null

        def deployment = new V1Deployment()
                .metadata(getObjectMetaModel(name))
                .spec(new V1DeploymentSpec(
                        new V1LabelSelector().matchLabels(["app": name]),
                        new V1PodTemplateSpec()
                                .metadata(getObjectMetaModel(null, ["app": name]))
                                .spec(new V1PodSpec([
                                        new V1Container(name)
                                                .image(image)
                                                .imagePullPolicy("Never")
                                                .volumeMounts(volumeMounts)
                                                .ports([new V1ContainerPort(port).name("http")])
                                                .livenessProbe(getProbeModel(getHTTPGetActionModel(new IntOrString(port), "/health/liveness"), 1, 3, 10))
                                                .readinessProbe(getProbeModel(getHTTPGetActionModel(new IntOrString(port), "/health/readiness"), 1, 3, 10))
                                ]).volumes(volumes))
                ).replicas(1)
                )

        createDeployment(appsV1Api, namespace, deployment)

        LOG.debug("Waiting until deployment is ready or timeout of 180s expires")

        Awaitility.await()
                .pollInterval(Duration.ofMillis(3))
                .atMost(Duration.ofSeconds(180))
                .until(() -> {
                    V1Deployment createdDeployment = getDeployment(appsV1Api, name, namespace)
                    return createdDeployment.getStatus().getReadyReplicas() > 0
                })
    }

    def createService(String name, int port) {
        V1ServiceSpec serviceSpec = getServiceSpecTypeModel("LoadBalancer", [getServicePortModel(port, new IntOrString(port))], ["app": name])
        V1Service service = getServiceModel(name, serviceSpec)

        LOG.debug("Creating Service: {}", service)

        createService(coreV1Api, namespace, service)

        LOG.debug("Waiting until service is ready or timeout of 180s expires")

        Awaitility.await()
                .pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(180))
                .until(() -> getEndpoints(coreV1Api, name, namespace) != null)
    }

    @Singleton
    @Requires(property = 'spec.type', value = 'example-test')
    static class NamespaceCleanupListener implements TestExecutionListener {

        @Inject
        @Shared
        @Value('${kubernetes.client.namespace:micronaut-kubernetes-openapi}')
        String namespace

        @Inject
        @Shared
        CoreV1Api coreV1Api

        void afterTestClass(TestContext testContext) throws Exception {
            deleteNamespaceSafe(coreV1Api, namespace)
        }
    }

    @Singleton
    @Replaces(DefaultKubeConfigLoader.class)
    @Requires(property = 'spec.type', value = 'example-test')
    static class TestKubeConfigLoader extends AbstractKubeConfigLoader {
        protected TestKubeConfigLoader(ResourceResolver resourceResolver) {
            super(resourceResolver)
        }

        @Override
        protected KubeConfig loadKubeConfig() {
            return new KubeConfig(TestUtils.KUBE_PROXY_CONFIG_MAP)
        }
    }
}
