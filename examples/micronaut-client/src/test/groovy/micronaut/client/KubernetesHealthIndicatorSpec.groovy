package micronaut.client

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires
import spock.lang.Specification

import javax.inject.Inject

import io.micronaut.context.annotation.Requires as MicronautRequires

@MicronautTest(environments = Environment.KUBERNETES)
@Property(name = "spec.name", value = "KubernetesHealthIndicatorSpec")
@Slf4j
class KubernetesHealthIndicatorSpec extends Specification implements KubectlCommands {

    @Inject
    ServiceClient client

    @Requires({ TestUtils.available("http://localhost:8888") })
    void "it works"() {
        when:
        Map details = client.health().details

        then:
        details.kubernetes.name == "micronaut-service"
        details.kubernetes.status == "UP"
        details.kubernetes.details.namespace == "micronaut-kubernetes"
        details.kubernetes.details.podName.startsWith "example-service"
        details.kubernetes.details.podPhase == "Running"
        details.kubernetes.details.podIP
        details.kubernetes.details.hostIP
        details.kubernetes.details.containerStatuses.first().name == "example-service"
        details.kubernetes.details.containerStatuses.first().image.endsWith "example-service:latest"
        details.kubernetes.details.containerStatuses.first().ready == true
    }

    @Client("http://localhost:9999")
    @MicronautRequires(property = "spec.name", value = "KubernetesHealthIndicatorSpec")
    static interface ServiceClient {

        @Get("/health")
        Map<String, Object> health()

    }


}