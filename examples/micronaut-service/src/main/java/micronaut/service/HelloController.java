package micronaut.service;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.HttpStatus;

@Controller("/hello")
public class HelloController {

    @Get("/{name}")
    public String index(final String name) {
        return "Hello, " + name;
    }
}