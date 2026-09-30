package micronaut.client

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.kubernetes.client.openapi.watcher.api.CoreV1ApiWatcher
import jakarta.inject.Inject
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory

@Controller("/secrets")
open class SecretController {

    companion object {
        private val LOG = LoggerFactory.getLogger(SecretController::class.java)
    }

    @Inject
    lateinit var coreV1ApiWatcher: CoreV1ApiWatcher

    @Get("/{namespace}")
    open fun startWatchingSecrets(@NotNull namespace: String) {
        coreV1ApiWatcher.listNamespacedSecret(namespace, null, null, null, null, null, null, null, null, null, null, null, true)
            .subscribe { event -> LOG.info(event.toString()) }
    }
}
