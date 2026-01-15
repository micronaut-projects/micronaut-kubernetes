package io.micronaut.kubernetes.client.openapi.watcher.mapper;

import io.micronaut.core.order.Ordered;

import java.util.Map;

public interface TypeNameMapper extends Ordered {

    Map<String, String> getMappings();
}
