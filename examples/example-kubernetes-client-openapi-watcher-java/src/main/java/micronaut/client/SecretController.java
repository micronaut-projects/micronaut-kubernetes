package micronaut.client;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.kubernetes.client.openapi.watcher.api.CoreV1ApiWatcher;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/secrets")
public class SecretController {
    private static final Logger LOG = LoggerFactory.getLogger(SecretController.class);

    @Inject
    CoreV1ApiWatcher coreV1ApiWatcher;

    @Get("/{namespace}")
    public void startWatchingSecrets(final @NotNull String namespace) {
        coreV1ApiWatcher.listNamespacedSecret(namespace, null, null, null, null, null, null, null, null, null, null, null, true)
            .subscribe(event -> LOG.info(event.toString()));
    }
}
