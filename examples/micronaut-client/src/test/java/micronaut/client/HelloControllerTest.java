package micronaut.client;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.kubernetes.test.EnabledIfAvailable;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
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
        assertEquals(testClient.all(), "[[\"example-client\",\"example-service\",\"kubernetes\",\"non-secure-service\",\"secure-service-labels\",\"secure-service-port-name\",\"secure-service-port-number\",\"compose-api\",\"kube-dns\",\"kubernetes-dashboard\"]]");
    }



    @Client("http://localhost:8888")
    public interface TestClient {

        @Get("/")
        String index();

        @Get("/all")
        String all();

    }

}
