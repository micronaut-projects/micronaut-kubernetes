package micronaut.client;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.HttpStatus;

@Controller("/")
public class HelloController {

    private ExampleClient client;

    public HelloController(ExampleClient client) {
        this.client = client;
    }

    @Get("/")
    public String index() {
        return client.hello("example-client");
    }
}