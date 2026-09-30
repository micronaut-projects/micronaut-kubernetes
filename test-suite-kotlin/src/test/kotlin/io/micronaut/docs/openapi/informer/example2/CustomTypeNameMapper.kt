package io.micronaut.docs.openapi.informer.example2

import io.micronaut.kubernetes.client.openapi.watcher.mapper.TypeNameMapper
import jakarta.inject.Singleton

//tag::get[]
@Singleton
class CustomTypeNameMapper : TypeNameMapper {
    override fun getMappings(): Map<String, String> {
        val mappings = HashMap<String, String>()
        mappings["io.micronaut.docs.openapi.informer.example2.CustomObjectCollection"] = "io.micronaut.docs.openapi.informer.example2.CustomObject"
        return mappings
    }
}
//end::get[]
