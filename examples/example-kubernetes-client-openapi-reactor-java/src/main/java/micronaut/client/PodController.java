package micronaut.client;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Controller("/pods")
public class PodController {

    @Inject
    CoreV1ApiReactor coreV1ApiReactor;

    @Get("/{namespace}/{name}")
    public Mono<String> getPod(final @NotNull String namespace, final @NotNull String name) {
        return coreV1ApiReactor.readNamespacedPod(name, namespace, null)
            .map(it -> it.getStatus().getPhase());
    }

    @Get("/{namespace}")
    public Mono<Map<String, String>> getPods(final @NotNull String namespace) {
        return coreV1ApiReactor.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null, null, null)
            .map(it -> it.getItems().stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.toMap(
                    p -> p.getMetadata().getName(),
                    p -> p.getStatus().getPhase())));
    }
}
