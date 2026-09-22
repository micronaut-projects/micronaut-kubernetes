package io.micronaut.docs.openapi.informer.example2

import io.micronaut.kubernetes.client.openapi.watcher.mapper.TypeNameMapper
import jakarta.inject.Singleton

//tag::get[]
@Singleton
class CustomTypeNameMapper implements TypeNameMapper {
    @Override
    Map<String, String> getMappings() {
        Map<String, String> mappings = new HashMap<>()
        mappings.put("io.micronaut.docs.openapi.informer.example2.CustomObjectCollection", "io.micronaut.docs.openapi.informer.example2.CustomObject")
        return mappings
    }
}
//end::get[]
