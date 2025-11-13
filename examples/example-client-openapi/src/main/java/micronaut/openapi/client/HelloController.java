package micronaut.openapi.client;

import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.reactivestreams.Publisher;

import java.util.List;

@Controller(produces = "text/plain", consumes = "test/plain")
@ExecuteOn(TaskExecutors.IO)
public class HelloController {

    private final ExampleClient client;
    private final DiscoveryClient discoveryClient;

    public HelloController(ExampleClient client, DiscoveryClient discoveryClient) {
        this.client = client;
        this.discoveryClient = discoveryClient;
    }

    @Get
    public String index() {
        return client.hello("example-client");
    }

    @Get("/all")
    @Produces(value = MediaType.APPLICATION_JSON)
    public Publisher<List<String>> all() {
        return discoveryClient.getServiceIds();
    }

    @Get("/enemies")
    public String enemies() {
        return client.enemiesCheatLevel();
    }

    @Get("/config/{key}")
    String config(String key) {
        return client.config(key);
    }

    @Post("/refreshService")
    String refresh() {
        return client.refresh();
    }

    @Get("/serviceEnv")
    String env() {
        return client.env();
    }
}
