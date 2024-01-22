package io.micronaut.kubernetes.client

import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.ServicePortBuilder
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1PodList
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.client.utils.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires
import spock.lang.Shared

import jakarta.inject.Inject

@MicronautTest
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "kubernetes-client")
@Property(name = "spec.reuseNamespace", value = "false")
class ApiClientFactorySpec extends KubernetesSpecification {

    @Inject
    @Shared
    PodsClient client

    def setupSpec(){
        operations.portForwardService("kubernetes-client-example", namespace, 8085, 8885)
    }

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
        operations.createRole("kubernetes-client", namespace)
        operations.createRoleBinding("kubernetes-client", namespace, "kubernetes-client")
        operations.createDeploymentFromFile(loadFileFromClasspath("k8s/kubernetes-client-example-deployment.yml"), "kubernetes-client-example", namespace)
        operations.createService("kubernetes-client-example", namespace,
                new ServiceSpecBuilder()
                        .withType("LoadBalancer")
                        .withPorts(
                                new ServicePortBuilder()
                                        .withPort(8085)
                                        .withTargetPort(new IntOrString(8085))
                                        .build()
                        )
                        .withSelector(["app": "kubernetes-client-example"])
                        .build())
    }

    def "test it can use kubeconfig"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.basePath": TestUtils.KUBEPROXY_BASE_PATH
        ], Environment.TEST)

        CoreV1Api coreV1Api = applicationContext.getBean(CoreV1Api)
        when:
        V1PodList podList = coreV1Api.listNamespacedPod("kube-system", null, null, null, null, null, null, null, null, null, null, null)

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
        podStatusMap.entrySet().getAt(0).getKey().startsWith("kubernetes-client-example")
        podStatusMap.entrySet().getAt(0).getValue() == "Running"
    }

    @Client(value = "http://localhost:8885", path = "/pods")
    static interface PodsClient {

        @Get("/{namespace}")
        Map<String, String> listPodStatuses(String namespace)
    }
}
