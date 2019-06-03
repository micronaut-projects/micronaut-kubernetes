package micronaut.client;

import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.reactivestreams.Publisher;

import java.util.List;

@Controller
public class HelloController {

    private ExampleClient client;
    private DiscoveryClient discoveryClient;

    public HelloController(ExampleClient client, DiscoveryClient discoveryClient) {
        this.client = client;
        this.discoveryClient = discoveryClient;
    }

    @Get
    public String index() {
        return client.hello("example-client");
    }

    @Get("/all")
    public Publisher<List<String>> all() {
        return discoveryClient.getServiceIds();
    }

    @Get("/enemies")
    public String enemies() {
        return client.enemiesCheatLevel();
    }
}