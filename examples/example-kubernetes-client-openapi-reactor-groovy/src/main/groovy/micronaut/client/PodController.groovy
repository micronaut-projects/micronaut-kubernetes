package micronaut.client

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1PodList
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import jakarta.inject.Inject
import jakarta.validation.constraints.NotNull
import reactor.core.publisher.Mono

@CompileStatic
@Controller("/pods")
class PodController {

    @Inject
    CoreV1ApiReactor coreV1ApiReactor

    @Get("/{namespace}/{name}")
    Mono<String> getPod(@NotNull String namespace, @NotNull String name) {
        return coreV1ApiReactor.readNamespacedPod(name, namespace, null)
            .map { V1Pod it -> it.status.phase }
    }

    @Get("/{namespace}")
    Mono<Map<String, String>> getPods(@NotNull String namespace) {
        return coreV1ApiReactor.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null, null, null)
            .map { V1PodList it ->
                it.items
                    .findAll { V1Pod p -> p.status != null }
                    .collectEntries { V1Pod p -> [(p.metadata.name): p.status.phase] } as Map<String, String>
            }
    }
}
