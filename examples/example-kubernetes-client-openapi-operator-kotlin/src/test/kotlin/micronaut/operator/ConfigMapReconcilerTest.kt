package micronaut.operator

import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ConfigMapReconcilerTest {

    private fun configMap(name: String) = V1ConfigMap().metadata(V1ObjectMeta().name(name).namespace("default"))

    private fun lister(vararg configMaps: V1ConfigMap): OperatorResourceLister<V1ConfigMap> {
        val indexer = mock(Indexer::class.java) as Indexer<V1ConfigMap>
        for (configMap in configMaps) {
            `when`(indexer.getByKey("default/" + configMap.metadata!!.name)).thenReturn(configMap)
        }
        val informer = mock(SharedIndexInformer::class.java) as SharedIndexInformer<V1ConfigMap>
        `when`(informer.indexer).thenReturn(indexer)
        val factory = mock(SharedIndexInformerFactory::class.java)
        `when`(factory.getExistingSharedIndexInformer(V1ConfigMap::class.java, "default")).thenReturn(informer)
        return OperatorResourceLister(factory, V1ConfigMap::class.java, false)
    }

    @Test
    fun testReconcileAnnotatesTheConfigMap() {
        val configMap = configMap("my-config")
        val lister = lister(configMap)
        val coreV1Api = mock(CoreV1Api::class.java)
        val reconciler = ConfigMapReconciler(coreV1Api)

        var result = reconciler.reconcile(Request("my-config", "default"), lister)

        assertFalse(result.requeue())
        assertEquals("processed", configMap.metadata!!.annotations!!["io.micronaut.operator"])
        verify(coreV1Api, times(1)).replaceNamespacedConfigMap("my-config", "default", configMap, null, null, null, null)

        // the annotation is already present, so the second reconciliation does not update the resource
        result = reconciler.reconcile(Request("my-config", "default"), lister)
        assertFalse(result.requeue())
        verify(coreV1Api, times(1)).replaceNamespacedConfigMap("my-config", "default", configMap, null, null, null, null)
    }

    @Test
    fun testReconcileOfADeletedConfigMap() {
        val coreV1Api = mock(CoreV1Api::class.java)

        val result = ConfigMapReconciler(coreV1Api).reconcile(Request("my-config", "default"), lister(configMap("other")))

        assertFalse(result.requeue())
        verify(coreV1Api, never()).replaceNamespacedConfigMap(any(), any(), any(), isNull(), isNull(), isNull(), isNull())
    }
}
