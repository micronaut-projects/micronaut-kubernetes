package micronaut.client

import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Requires
import spock.lang.Specification

import javax.inject.Inject

import io.micronaut.context.annotation.Requires as MicronautRequires

@MicronautTest(environments = Environment.KUBERNETES)
@Property(name = "spec.name", value = "KubernetesHealthIndicatorSpec")
class KubernetesHealthIndicatorSpec extends Specification implements KubectlCommands {

    @Inject
    ServiceClient client

    @Requires({ TestUtils.available("http://localhost:8888") })
    void "it works"() {
        expect:
        client.health().details.kubernetes.name == "micronaut-service"
        client.health().details.kubernetes.status == "UP"
        client.health().details.kubernetes.details.namespace == "default"
        client.health().details.kubernetes.details.podName.startsWith "example-service"
        client.health().details.kubernetes.details.podPhase == "Running"
        client.health().details.kubernetes.details.podIP
        client.health().details.kubernetes.details.hostIP
        client.health().details.kubernetes.details.containerStatuses.first().name == "example-service"
        client.health().details.kubernetes.details.containerStatuses.first().image == "registry.hub.docker.com/alvarosanchez/example-service:latest"
        client.health().details.kubernetes.details.containerStatuses.first().ready == true
    }

    @Client("http://localhost:9999")
    @MicronautRequires(property = "spec.name", value = "KubernetesHealthIndicatorSpec")
    static interface ServiceClient {

        @Get("/health")
        Map<String, Object> health()

    }


}