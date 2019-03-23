package micronaut.client;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;

@Client("example-service")
public interface ExampleClient {

    @Get("/hello/{name}")
    String hello(String name);
}