package micronaut.service;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.runtime.context.scope.Refreshable;
import jakarta.inject.Inject;

@Controller(value = "/config", produces = MediaType.TEXT_PLAIN, consumes = MediaType.TEXT_PLAIN)
@Refreshable
public class ConfigImportController {

    @Inject
    ApplicationContext context;

    @ReflectiveAccess
    @Value("${test-count:0}")
    private Integer count;

    @Get("/var/count")
    public Integer count() {
        return count;
    }

    @Get("/context/{key}")
    public String getConfigValue(String key) {
        return context.get(key, String.class).orElse("NOTHING");
    }
}
