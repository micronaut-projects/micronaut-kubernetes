package micronaut.client

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.model.V1PodList
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import jakarta.inject.Inject
import jakarta.validation.constraints.NotNull

@CompileStatic
@Controller("/pods")
@ExecuteOn(TaskExecutors.BLOCKING)
class PodController {

    @Inject
    CoreV1Api coreV1Api

    @Get("/{namespace}/{name}")
    String getPod(@NotNull String namespace, @NotNull String name) {
        V1Pod v1Pod = coreV1Api.readNamespacedPod(name, namespace, null)
        return v1Pod.status.phase
    }

    @Get("/{namespace}")
    Map<String, String> getPods(@NotNull String namespace) {
        V1PodList v1PodList = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null, null, null)
        return v1PodList.items
            .findAll { V1Pod p -> p.status != null }
            .collectEntries { V1Pod p -> [(p.metadata.name): p.status.phase] } as Map<String, String>
    }
}
