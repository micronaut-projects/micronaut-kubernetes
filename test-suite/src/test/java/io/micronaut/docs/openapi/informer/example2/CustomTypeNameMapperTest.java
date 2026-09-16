package io.micronaut.docs.openapi.informer.example2;

import io.micronaut.kubernetes.client.openapi.watcher.mapper.TypeNameMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class CustomTypeNameMapperTest {

    @Inject
    List<TypeNameMapper> typeNameMappers;

    @Test
    void testTheMapperIsRegistered() {
        TypeNameMapper mapper = typeNameMappers.stream()
            .filter(CustomTypeNameMapper.class::isInstance)
            .findFirst()
            .orElseThrow();

        assertEquals("io.micronaut.docs.openapi.informer.example2.CustomObject",
            mapper.getMappings().get("io.micronaut.docs.openapi.informer.example2.CustomObjectCollection"));
        assertTrue(mapper.getMappings().size() == 1);
    }
}
