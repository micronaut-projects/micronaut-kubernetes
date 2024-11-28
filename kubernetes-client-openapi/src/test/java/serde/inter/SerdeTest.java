package serde.inter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@MicronautTest
public class SerdeTest {

    @Inject
    JsonMapper jsonMapper;

    @Test
    void testDeserialization() throws IOException {
        String errorJson = """
            {"type":"ERROR","object":{"kind":"Status","apiVersion":"v1","status":"Failure","message":"too old resource version: 1200 (184080)","reason":"Expired"}}
            """;

        String validJson = """
            {"type":"ADDED","object":{"kind":"Namespace","apiVersion":"v1","spec":{"finalizers":["kubernetes"]},"status":{"phase":"Active"}}}
            """;

        ObjectMapper objectMapper = new ObjectMapper();
        WatchEvent jacksonErrorEvent = objectMapper.readValue(errorJson, WatchEvent.class);
        WatchEvent jacksonValidEvent = objectMapper.readValue(validJson, WatchEvent.class);

        WatchEvent micronautErrorEvent = jsonMapper.readValue(errorJson, WatchEvent.class);
        WatchEvent micronautValidEvent = jsonMapper.readValue(validJson, WatchEvent.class);
    }

}
