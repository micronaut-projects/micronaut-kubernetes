package io.micronaut.docs.informer

import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ObjectMeta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigMapInformerTest {

    private fun configMap(name: String) = V1ConfigMap().metadata(V1ObjectMeta().name(name).namespace("default"))

    @Test
    fun testTheHandlerRecordsTheEvents() {
        val informer = ConfigMapInformer()

        informer.onAdd(configMap("added"))
        informer.onUpdate(configMap("added"), configMap("updated"))
        informer.onDelete(configMap("deleted"), false)

        assertEquals(listOf("added"), informer.added.map { it.metadata!!.name })
        assertEquals(listOf("updated"), informer.updated.map { it.metadata!!.name })
        assertEquals(listOf("deleted"), informer.deleted.map { it.metadata!!.name })
    }
}
