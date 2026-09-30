package io.micronaut.docs.operator

import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ObjectMeta
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OperatorFiltersTest {

    private fun configMap(annotations: Map<String, String>?) =
        V1ConfigMap().metadata(V1ObjectMeta().name("test").namespace("default").annotations(annotations?.toMutableMap()))

    @Test
    fun testOnAddFilter() {
        assertTrue(OnAddFilter().test(configMap(mapOf("io.micronaut.operator" to "processed"))))
        assertFalse(OnAddFilter().test(configMap(mapOf("other" to "value"))))
        assertFalse(OnAddFilter().test(configMap(null)))
    }

    @Test
    fun testOnUpdateFilter() {
        assertTrue(OnUpdateFilter().test(configMap(null), configMap(mapOf("io.micronaut.operator" to "processed"))))
        assertFalse(OnUpdateFilter().test(configMap(mapOf("io.micronaut.operator" to "processed")), configMap(null)))
    }

    @Test
    fun testOnDeleteFilter() {
        assertTrue(OnDeleteFilter().test(configMap(mapOf("io.micronaut.operator" to "processed")), false))
        assertFalse(OnDeleteFilter().test(configMap(null), true))
    }
}
