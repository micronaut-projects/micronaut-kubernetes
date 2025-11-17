package micronaut.openapi.client;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

@Client("example-service")
public interface ExampleClient {

    @Get(uri = "/hello/{name}", processes = MediaType.TEXT_PLAIN)
    String hello(String name);

    @Get(uri = "/hello/enemies", processes = MediaType.TEXT_PLAIN)
    String enemiesCheatLevel();

    @Get(uri = "/hello/config/{key}", processes = MediaType.TEXT_PLAIN)
    String config(String key);

    @Post("/refresh")
    String refresh();

    @Get("/env")
    String env();
}
