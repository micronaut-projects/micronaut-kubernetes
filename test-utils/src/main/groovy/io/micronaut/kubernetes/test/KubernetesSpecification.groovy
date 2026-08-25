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

import groovy.util.logging.Slf4j
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Namespace
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.VersionInfo
import io.micronaut.context.annotation.Value
import io.micronaut.core.io.ResourceResolver
import io.micronaut.core.io.scan.ClassPathResourceLoader
import jakarta.inject.Inject
import org.spockframework.runtime.extension.IMethodInterceptor
import spock.lang.Shared
import spock.lang.Specification

import static io.micronaut.kubernetes.test.KubernetesModels.getServicePortModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServiceSpecTypeModel
import static io.micronaut.kubernetes.test.KubernetesOperations.createConfigMap
import static io.micronaut.kubernetes.test.KubernetesOperations.createConfigMapFromFile
import static io.micronaut.kubernetes.test.KubernetesOperations.createDeploymentFromFile
import static io.micronaut.kubernetes.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.test.KubernetesOperations.createRole
import static io.micronaut.kubernetes.test.KubernetesOperations.createRoleBinding
import static io.micronaut.kubernetes.test.KubernetesOperations.createSecret
import static io.micronaut.kubernetes.test.KubernetesOperations.createService
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteNamespace
import static io.micronaut.kubernetes.test.KubernetesOperations.getNamespace
import static io.micronaut.kubernetes.test.KubernetesOperations.getVersionInfo

/**
 * Abstract specification encapsulating setup of test namespace.
 *
 * @author Pavol Gressa
 * @since 2.3
 */
@Slf4j
abstract class KubernetesSpecification extends Specification {

    private static final int FAILURE_LOG_TAIL_LINES = 500

    public static final String EXAMPLE_SERVICE_DEPLOYMENT = "k8s/example-service-deployment.yml"

    @Shared
    @Value('${kubernetes.client.namespace:micronaut-kubernetes}')
    @Inject
    String namespace

    @Shared
    @Value('${spec.reuseNamespace:true}')
    @Inject
    boolean reuseNamespace

    /**
     * Setup the fixture in namespace.
     * @param namespace
     * @return
     */
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
        createBaseResources(namespace)
        createExampleServiceDeployment(namespace)
        createExampleClientDeployment(namespace)
        createSecureDeployment(namespace)
    }

    def setupSpec() {
        addPodLogOnFailureInterceptor()
        VersionInfo versionInfo = getVersionInfo()
        log.info("Using Kubernetes version: ${versionInfo.major}.${versionInfo.minor}")
        if (reuseNamespace && getNamespaceOpt(namespace).isPresent()) {
            log.info("Reusing namespace ${namespace}")
        } else {
            log.info("Configuring namespace: ${namespace}")
            setupFixture(namespace)
        }
    }

    /**
     * Creates namespace. If such namespace already exists then the namespace is deleted first.
     * @param namespace namespace name
     */
    def createNamespaceSafe(String namespace) {
        if (getNamespaceOpt(namespace).isPresent()) {
            deleteNamespace(namespace)
        }
        createNamespace(namespace)
    }

    def cleanupSpec() {
        if (reuseNamespace && getNamespaceOpt(namespace).isPresent()) {
            log.info("Skipping cleanup of namespace ${namespace}")
        } else {
            log.info("Cleaning up namespace ${namespace}")
            deleteNamespace(namespace)
        }
    }

    /**
     * Logs the last lines from every container in the test namespace. This is invoked only after
     * a feature fails, so it does not add noise to successful Kubernetes integration test output.
     */
    protected void logPodLogs() {
        CoreV1Api coreV1Api = new CoreV1Api()
        try {
            coreV1Api.listNamespacedPod(namespace).execute().items.each { V1Pod pod ->
                pod.spec?.containers?.each { container ->
                    try {
                        String podLogs = coreV1Api.readNamespacedPodLog(pod.metadata.name, namespace)
                            .container(container.name)
                            .tailLines(FAILURE_LOG_TAIL_LINES)
                            .execute()
                        log.error("***** Pod {}/{} Log Start *****\n{}***** Pod {}/{} Log End *****",
                                pod.metadata.name,
                                container.name,
                                podLogs,
                                pod.metadata.name,
                                container.name)
                    } catch (Exception e) {
                        // Do not mask the original feature failure when diagnostic log retrieval fails.
                        log.warn("Unable to retrieve logs for pod {}/{}", pod.metadata.name, container.name, e)
                    }
                }
            }
        } catch (Exception e) {
            // Do not mask the original feature failure when listing pods fails.
            log.warn("Unable to list pods in namespace {} for diagnostic logs", namespace, e)
        }
    }

    private void addPodLogOnFailureInterceptor() {
        IMethodInterceptor interceptor = { invocation ->
            try {
                invocation.proceed()
            } catch (Throwable e) {
                logPodLogs()
                throw e
            }
        } as IMethodInterceptor
        specificationContext.currentSpec.allFeatures.each { feature ->
            feature.featureMethod.addInterceptor(interceptor)
        }
    }

    def createBaseResources(String namespace) {
        createRole("service-discoverer", namespace)
        createRoleBinding("default-service-discoverer", namespace, "service-discoverer")

        createConfigMapFromFile("game-config-properties", namespace,
            loadFileFromClasspath("k8s/game.properties"))
        createConfigMapFromFile("game-config-yml", namespace,
            loadFileFromClasspath("k8s/game.yml"),
            ["app": "game", "app.kubernetes.io/instance": "example-service-1337"])
        createConfigMapFromFile("game-config-json", namespace,
            loadFileFromClasspath("k8s/game.json"))
        createConfigMapFromFile("mounted-configmap", namespace,
            loadFileFromClasspath("k8s/mounted.yml"))
        createConfigMap("literal-config", namespace,
            ["special.how": "very", "special.type": "charm"],
            ["app": "game", "app.kubernetes.io/instance": "example-service-1337"])

        createSecret("test-secret", namespace,
            ["username": "my-app".bytes, "password": "39528\$vdg7Jb".bytes])
        createSecret("another-secret", namespace,
            ["secretProperty": "secretValue".bytes],
            ["app": "game", "app.kubernetes.io/instance": "example-service-1337"])
        createSecret("mounted-secret", namespace,
            ["mountedVolumeKey": "mountedVolumeValue".bytes])
    }

    def createExampleServiceDeployment(String namespace) {
        createDeploymentFromFile(loadFileFromClasspath(EXAMPLE_SERVICE_DEPLOYMENT), "example-service", namespace)
        createService(
            "example-service",
            namespace,
            getServiceSpecTypeModel("LoadBalancer", [getServicePortModel(8081, 8081)], ["app": "example-service"]),
            ["foo": "bar"])
    }

    def createExampleClientDeployment(String namespace) {
        createDeploymentFromFile(loadFileFromClasspath("k8s/example-client-deployment.yml"), "example-client", namespace)
        createService(
            "example-client",
            namespace,
            getServiceSpecTypeModel("LoadBalancer", [getServicePortModel(8082, 8082)], ["app": "example-client"]))
    }

    def createSecureDeployment(String namespace) {
        createDeploymentFromFile(loadFileFromClasspath("k8s/secure-deployment.yml"), "secure-deployment", namespace)
        createService(
            "secure-service-port-name",
            namespace,
            getServiceSpecTypeModel("NodePort", [getServicePortModel("https", 1234)], ["app": "example-service"]))

        createService(
            "secure-service-port-number",
            namespace,
            getServiceSpecTypeModel("NodePort", [getServicePortModel(443)], ["app": "secure-deployment"]))

        createService(
            "secure-service-labels",
            namespace,
            getServiceSpecTypeModel("NodePort", [getServicePortModel(1234)], ["app": "secure-deployment"]),
            ["secure": "true"])

        createService(
            "non-secure-service",
            namespace,
            getServiceSpecTypeModel("NodePort", [getServicePortModel(1234)], ["app": "secure-deployment"]))
    }

    static Optional<V1Namespace> getNamespaceOpt(String namespace) {
        try {
            return Optional.of(getNamespace(namespace))
        } catch (ApiException e) {
            if (e.code == 404) {
                return Optional.empty()
            }
            throw e
        }
    }

    protected static URL loadFileFromClasspath(String path) {
        ClassPathResourceLoader loader = new ResourceResolver().getLoader(ClassPathResourceLoader.class).get()
        Optional<URL> resource = loader.getResource("classpath:${path}")
        return resource.orElseThrow(
            () -> new IllegalArgumentException("File ${path} not found on classpath!"))
    }
}
