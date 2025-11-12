package micronaut.openapi.service;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.runtime.context.scope.Refreshable;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Inject;

@Controller(value = "/hello", produces = MediaType.TEXT_PLAIN, consumes = MediaType.TEXT_PLAIN)
@Refreshable
public class HelloController {

    @Inject
    EmbeddedServer embeddedServer;

    @Inject
    ApplicationContext context;

    @ReflectiveAccess
    @Value("${enemies.cheat.level:defaultCheatLevel}")
    private String enemiesCheatLevel;

    @Get("/{name}")
    public String index(final String name) {
        return "Hello, " + name + ". From " + embeddedServer.getHost() + ":" + embeddedServer.getPort() + "\n";
    }

    @Get("/enemies")
    public String enemies() {
        return enemiesCheatLevel;
    }

    @Get("/config/{key}")
    public String config(String key) {
        return context.get(key, String.class).orElse("NOTHING");
    }
}
