package micronaut.client;

import io.micronaut.context.env.Environment;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.kubernetes.test.EnabledIfAvailable;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(environments = Environment.KUBERNETES)
public class HelloControllerTest {

    @Inject
    TestClient testClient;

    @Test
    @EnabledIfAvailable("http://localhost:8888")
    public void testIndex() {
        assertTrue(testClient.index().startsWith("Hello, example-client"));
    }

    @Test
    @EnabledIfAvailable("http://localhost:8888")
    public void testAll() {
        assertTrue(testClient.all().contains("example-service"));
    }

    @Test
    @EnabledIfAvailable("http://localhost:8888")
    public void testEnemies() {
        assertTrue(testClient.enemies().equals("noGoodRotten"));
    }



    @Client("http://localhost:8888")
    public interface TestClient {

        @Get
        String index();

        @Get("/all")
        String all();

        @Get("/enemies")
        String enemies();

    }

}
