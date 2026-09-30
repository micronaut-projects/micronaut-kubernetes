package io.micronaut.docs.openapi.informer.example2

import io.micronaut.kubernetes.client.openapi.watcher.mapper.TypeNameMapper
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class CustomTypeNameMapperSpec extends Specification {

    @Inject
    List<TypeNameMapper> typeNameMappers

    void "the mapper is registered"() {
        given:
        TypeNameMapper mapper = typeNameMappers.find { it instanceof CustomTypeNameMapper }

        expect:
        mapper.mappings == ["io.micronaut.docs.openapi.informer.example2.CustomObjectCollection": "io.micronaut.docs.openapi.informer.example2.CustomObject"]
    }
}
