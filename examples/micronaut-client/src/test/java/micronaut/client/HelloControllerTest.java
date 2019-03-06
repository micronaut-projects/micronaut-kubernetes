package micronaut.client;

import io.micronaut.http.client.RxHttpClient;
import io.micronaut.kubernetes.test.EnabledIfAvailable;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;

import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class HelloControllerTest {

    @Inject
    EmbeddedServer embeddedServer;

    @Test
    @EnabledIfAvailable("http://localhost:8888")
    public void testIndex() {
        try(RxHttpClient client = embeddedServer.getApplicationContext().createBean(RxHttpClient.class, embeddedServer.getURL())) {
            assertTrue(client.toBlocking().exchange("/", String.class).body().startsWith("Hello, example-client"));
        }
    }
}
