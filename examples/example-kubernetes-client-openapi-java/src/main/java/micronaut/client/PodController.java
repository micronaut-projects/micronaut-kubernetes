package micronaut.client;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api;
import io.micronaut.kubernetes.client.openapi.model.V1Pod;
import io.micronaut.kubernetes.client.openapi.model.V1PodList;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

@Controller("/pods")
@ExecuteOn(TaskExecutors.BLOCKING)
public class PodController {

    @Inject
    CoreV1Api coreV1Api;

    @Get("/{namespace}/{name}")
    public String getPod(final @NotNull String namespace, final @NotNull String name) {
        V1Pod v1Pod = coreV1Api.readNamespacedPod(name, namespace, null);
        return v1Pod.getStatus().getPhase();
    }

    @Get("/{namespace}")
    public Map<String, String> getPods(final @NotNull String namespace) {
        V1PodList v1PodList = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null, false);
        return v1PodList.getItems().stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.toMap(
                        p -> p.getMetadata().getName(),
                        p -> p.getStatus().getPhase()));
    }
}
