package micronaut.client.configmap

import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ConfigMapControllerTest {

    private fun configMap(name: String) = V1ConfigMap().metadata(V1ObjectMeta().name(name).namespace("default"))

    private fun controller(vararg configMaps: V1ConfigMap): ConfigMapController {
        val indexer = mock(Indexer::class.java) as Indexer<V1ConfigMap>
        `when`(indexer.list()).thenReturn(configMaps.toList())
        for (configMap in configMaps) {
            `when`(indexer.getByKey("default/" + configMap.metadata!!.name)).thenReturn(configMap)
        }
        val informer = mock(SharedIndexInformer::class.java) as SharedIndexInformer<V1ConfigMap>
        `when`(informer.indexer).thenReturn(indexer)
        val factory = mock(SharedIndexInformerFactory::class.java)
        `when`(factory.getExistingSharedIndexInformer(V1ConfigMap::class.java, "default")).thenReturn(informer)
        val namespaceResolver = mock(NamespaceResolver::class.java)
        `when`(namespaceResolver.resolveNamespace()).thenReturn("default")
        return ConfigMapController(factory, namespaceResolver)
    }

    @Test
    fun testAllReturnsTheCachedConfigMaps() {
        val controller = controller(configMap("first"), configMap("second"))

        assertEquals(listOf("first", "second"), controller.all().map { it.metadata!!.name })
    }

    @Test
    fun testConfigMapIsLookedUpByKey() {
        val controller = controller(configMap("first"))

        assertEquals("first", controller.configMap("first")!!.metadata!!.name)
        assertNull(controller.configMap("missing"))
    }
}
