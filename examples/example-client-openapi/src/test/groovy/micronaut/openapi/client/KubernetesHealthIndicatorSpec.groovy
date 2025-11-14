package micronaut.openapi.client

import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.openapi.test.KubectlPortForward
import io.micronaut.kubernetes.openapi.test.KubernetesSpecification
import io.micronaut.kubernetes.openapi.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared

import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.portForwardService

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "spec.type", value = "example-test")
@Property(name = "spec.name", value = "KubernetesHealthIndicatorSpec")
@Property(name = "kubernetes.client.namespace", value = "example-openapi-discovery")
class KubernetesHealthIndicatorSpec extends KubernetesSpecification {

    @Inject
    @Shared
    ServiceClient client

    @Shared
    @AutoCleanup
    KubectlPortForward kubectlPortForward

    def setupSpec() {
        kubectlPortForward = portForwardService(coreV1Api, "example-service", namespace, 8081, 9999)
    }

    @Override
    def createResources() {
        createDeployment("example-service", "micronaut-kubernetes-example-service-openapi", 8081, false)
        createService("example-service", 8081)
    }

    void "health test"() {
        when:
        Map details = client.health().details

        then:
        details.kubernetes.name == "micronaut-kubernetes-example-service-openapi"
        details.kubernetes.status == "UP"
        details.kubernetes.details.namespace == namespace
        details.kubernetes.details.podName.startsWith "example-service"
        details.kubernetes.details.podPhase == "Running"
        details.kubernetes.details.podIP
        details.kubernetes.details.hostIP
        details.kubernetes.details.containerStatuses.first().name == "example-service"
        details.kubernetes.details.containerStatuses.first().image.endsWith "example-service-openapi:latest"
        details.kubernetes.details.containerStatuses.first().ready == true
    }

    @Client("http://localhost:9999")
    @io.micronaut.context.annotation.Requires(property = "spec.name", value = "KubernetesHealthIndicatorSpec")
    static interface ServiceClient {

        @Get("/health")
        Map<String, Object> health()

    }
}
