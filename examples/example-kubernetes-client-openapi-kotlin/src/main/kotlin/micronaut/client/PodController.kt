package micronaut.client

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import jakarta.inject.Inject
import jakarta.validation.constraints.NotNull

@Controller("/pods")
@ExecuteOn(TaskExecutors.BLOCKING)
open class PodController {

    @Inject
    lateinit var coreV1Api: CoreV1Api

    @Get("/{namespace}/{name}")
    open fun getPod(@NotNull namespace: String, @NotNull name: String): String {
        val v1Pod = coreV1Api.readNamespacedPod(name, namespace, null)
        return v1Pod.status!!.phase!!
    }

    @Get("/{namespace}")
    open fun getPods(@NotNull namespace: String): Map<String, String> {
        val v1PodList = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null, null, null)
        return v1PodList.items
            .filter { it.status != null }
            .associate { it.metadata!!.name!! to it.status!!.phase!! }
    }
}
