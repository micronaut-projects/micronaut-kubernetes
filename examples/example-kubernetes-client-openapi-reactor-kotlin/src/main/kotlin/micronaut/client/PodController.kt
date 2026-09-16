package micronaut.client

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import jakarta.inject.Inject
import jakarta.validation.constraints.NotNull
import reactor.core.publisher.Mono

@Controller("/pods")
open class PodController {

    @Inject
    lateinit var coreV1ApiReactor: CoreV1ApiReactor

    @Get("/{namespace}/{name}")
    open fun getPod(@NotNull namespace: String, @NotNull name: String): Mono<String> {
        return coreV1ApiReactor.readNamespacedPod(name, namespace, null)
            .map { it.status!!.phase!! }
    }

    @Get("/{namespace}")
    open fun getPods(@NotNull namespace: String): Mono<Map<String, String>> {
        return coreV1ApiReactor.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null, null, null)
            .map { list ->
                list.items
                    .filter { it.status != null }
                    .associate { it.metadata!!.name!! to it.status!!.phase!! }
            }
    }
}
