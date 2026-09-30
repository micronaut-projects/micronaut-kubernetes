package io.micronaut.docs.openapi.informer.example2

import io.micronaut.kubernetes.client.openapi.watcher.mapper.TypeNameMapper
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest
class CustomTypeNameMapperTest {

    @Inject
    lateinit var typeNameMappers: List<TypeNameMapper>

    @Test
    fun testTheMapperIsRegistered() {
        val mapper = typeNameMappers.first { it is CustomTypeNameMapper }

        assertEquals("io.micronaut.docs.openapi.informer.example2.CustomObject",
            mapper.mappings["io.micronaut.docs.openapi.informer.example2.CustomObjectCollection"])
        assertEquals(1, mapper.mappings.size)
    }
}
