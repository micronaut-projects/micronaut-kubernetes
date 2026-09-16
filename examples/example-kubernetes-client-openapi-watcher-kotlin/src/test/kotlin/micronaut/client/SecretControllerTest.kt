package micronaut.client

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

@MicronautTest
class SecretControllerTest {

    @Inject
    lateinit var controller: SecretController

    @Test
    fun testCoreV1ApiWatcherIsInjected() {
        assertNotNull(controller.coreV1ApiWatcher)
    }
}
