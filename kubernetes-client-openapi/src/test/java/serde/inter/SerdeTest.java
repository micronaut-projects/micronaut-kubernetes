package serde.inter;

import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.kubernetes.client.openapi.model.V1Namespace;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@MicronautTest
public class SerdeTest {

    @Inject
    JsonMapper jsonMapper;

    @Test
    void testAddedEventDeserialization() throws IOException {
        String addedEvent = """
            {"type":"ADDED","object":{"kind":"Namespace","apiVersion":"v1","spec":{"finalizers":["kubernetes"]},"status":{"phase":"Active"}}}
            """;

        WatchEvent<V1Namespace> namespaceWatchEvent = jsonMapper.readValue(addedEvent, Argument.of(WatchEvent.class, Argument.of(V1Namespace.class)));

        Assertions.assertTrue(namespaceWatchEvent.object() instanceof V1Namespace);
    }

    @Test
    void testErrorEventDeserialization() throws IOException {
        String errorEvent = """
            {"type":"ERROR","object":{"kind":"Status","apiVersion":"v1","status":"Failure","message":"too old resource version: 1200 (184080)","reason":"Expired"}}
            """;

        WatchEvent<V1Namespace> namespaceWatchEvent = jsonMapper.readValue(errorEvent, Argument.of(WatchEvent.class, Argument.of(V1Namespace.class)));

        Assertions.assertNotNull(namespaceWatchEvent.status());
    }
}
