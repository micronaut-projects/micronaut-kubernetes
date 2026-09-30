package micronaut.client

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

@MicronautTest
class PodControllerTest {

    @Inject
    lateinit var controller: PodController

    @Test
    fun testCoreV1ApiReactorIsInjected() {
        assertNotNull(controller.coreV1ApiReactor)
    }
}
