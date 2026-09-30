package micronaut.client

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class PodControllerSpec extends Specification {

    @Inject
    PodController controller

    void "the CoreV1Api is injected"() {
        expect:
        controller.coreV1Api != null
    }
}
