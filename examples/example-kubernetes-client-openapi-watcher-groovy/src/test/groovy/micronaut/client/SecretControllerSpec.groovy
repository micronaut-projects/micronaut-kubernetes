package micronaut.client

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class SecretControllerSpec extends Specification {

    @Inject
    SecretController controller

    void "the CoreV1ApiWatcher is injected"() {
        expect:
        controller.coreV1ApiWatcher != null
    }
}
