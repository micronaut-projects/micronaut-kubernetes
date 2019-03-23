package micronaut.service;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.runtime.server.EmbeddedServer;

import javax.inject.Inject;

@Controller("/hello")
public class HelloController {

    @Inject
    EmbeddedServer embeddedServer;

    @Get("/{name}")
    public String index(final String name) {
        return "Hello, " + name + ". From " + embeddedServer.getHost() + ":" + embeddedServer.getPort() + "\n";
    }
}