package io.micronaut.docs.operator;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorFiltersTest {

    private static V1ConfigMap configMap(Map<String, String> annotations) {
        return new V1ConfigMap().metadata(new V1ObjectMeta().name("test").namespace("default").annotations(annotations));
    }

    @Test
    void testOnAddFilter() {
        assertTrue(new OnAddFilter().test(configMap(Map.of("io.micronaut.operator", "processed"))));
        assertFalse(new OnAddFilter().test(configMap(Map.of("other", "value"))));
        assertFalse(new OnAddFilter().test(configMap(null)));
    }

    @Test
    void testOnUpdateFilter() {
        assertTrue(new OnUpdateFilter().test(configMap(null), configMap(Map.of("io.micronaut.operator", "processed"))));
        assertFalse(new OnUpdateFilter().test(configMap(Map.of("io.micronaut.operator", "processed")), configMap(null)));
    }

    @Test
    void testOnDeleteFilter() {
        assertTrue(new OnDeleteFilter().test(configMap(Map.of("io.micronaut.operator", "processed")), false));
        assertFalse(new OnDeleteFilter().test(configMap(null), true));
    }
}
