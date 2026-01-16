package io.micronaut.kubernetes.client.openapi.informer.example2;

import io.micronaut.kubernetes.client.openapi.watcher.mapper.TypeNameMapper;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class CustomTypeNameMapper implements TypeNameMapper {
    @NonNull
    @Override
    public Map<String, String> getMappings() {
        Map<String, String> mappings = new HashMap<>();
        mappings.put("io.micronaut.kubernetes.client.openapi.informer.example2.CustomObjectCollection", "io.micronaut.kubernetes.client.openapi.informer.example2.CustomObject");
        return mappings;
    }
}
