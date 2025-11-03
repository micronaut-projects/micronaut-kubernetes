package io.micronaut.kubernetes.client

import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1PodList
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.test.KubectlPortForward
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared

import static io.micronaut.kubernetes.test.KubernetesModels.getServicePortModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServiceSpecTypeModel
import static io.micronaut.kubernetes.test.KubernetesOperations.createDeploymentFromFile
import static io.micronaut.kubernetes.test.KubernetesOperations.createRole
import static io.micronaut.kubernetes.test.KubernetesOperations.createRoleBinding
import static io.micronaut.kubernetes.test.KubernetesOperations.createService
import static io.micronaut.kubernetes.test.KubernetesOperations.portForwardService

@MicronautTest
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "kubernetes-client")
@Property(name = "spec.reuseNamespace", value = "false")
class ApiClientFactorySpec extends KubernetesSpecification {

    @Inject
    @Shared
    PodsClient client

    @Shared
    @AutoCleanup
    KubectlPortForward kubectlPortForward

    def setupSpec() {
        kubectlPortForward = portForwardService("kubernetes-client-example", namespace, 8085, 8885)
    }

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
        createRole("kubernetes-client", namespace)
        createRoleBinding("kubernetes-client", namespace, "kubernetes-client")
        createDeploymentFromFile(loadFileFromClasspath("k8s/kubernetes-client-example-deployment.yml"), "kubernetes-client-example", namespace)
        createService("kubernetes-client-example",
            namespace,
            getServiceSpecTypeModel("LoadBalancer", [getServicePortModel(8085, 8085)], ["app": "kubernetes-client-example"]))
    }

    def "test it can use kubeconfig"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.basePath": TestUtils.KUBEPROXY_BASE_PATH
        ], Environment.TEST)

        CoreV1Api coreV1Api = applicationContext.getBean(CoreV1Api)
        when:
        V1PodList podList = coreV1Api.listNamespacedPod("kube-system").execute()

        then:
        podList
        !podList.items.isEmpty()
    }

    def "test it runs in cluster"() {
        when:
        Map<String, String> podStatusMap = client.listPodStatuses(namespace)

        then:
        podStatusMap
        !podStatusMap.isEmpty()
        podStatusMap.size() == 1
        podStatusMap.entrySet()[0].getKey().startsWith("kubernetes-client-example")
        podStatusMap.entrySet()[0].getValue() == "Running"
    }

    @Client(value = "http://localhost:8885", path = "/pods")
    static interface PodsClient {

        @Get("/{namespace}")
        Map<String, String> listPodStatuses(String namespace)
    }
}
