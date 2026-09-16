package micronaut.operator

import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request
import spock.lang.Specification

class ConfigMapReconcilerSpec extends Specification {

    private static V1ConfigMap configMap(String name) {
        new V1ConfigMap().metadata(new V1ObjectMeta().name(name).namespace("default"))
    }

    private OperatorResourceLister<V1ConfigMap> lister(V1ConfigMap... configMaps) {
        Indexer<V1ConfigMap> indexer = Stub(Indexer) {
            getByKey(_) >> { String key -> configMaps.find { "default/" + it.metadata.name == key } }
        }
        SharedIndexInformer<V1ConfigMap> informer = Stub(SharedIndexInformer) {
            getIndexer() >> indexer
        }
        SharedIndexInformerFactory factory = Stub(SharedIndexInformerFactory) {
            getExistingSharedIndexInformer(V1ConfigMap, "default") >> informer
        }
        new OperatorResourceLister<V1ConfigMap>(factory, V1ConfigMap, false)
    }

    void "reconcile annotates the config map"() {
        given:
        V1ConfigMap configMap = configMap("my-config")
        OperatorResourceLister<V1ConfigMap> lister = lister(configMap)
        CoreV1Api coreV1Api = Mock()
        ConfigMapReconciler reconciler = new ConfigMapReconciler(coreV1Api)

        when:
        def result = reconciler.reconcile(new Request("my-config", "default"), lister)

        then:
        !result.requeue()
        configMap.metadata.annotations["io.micronaut.operator"] == "processed"
        1 * coreV1Api.replaceNamespacedConfigMap("my-config", "default", configMap, null, null, null, null)

        when: "the annotation is already present"
        result = reconciler.reconcile(new Request("my-config", "default"), lister)

        then: "the resource is not updated again"
        !result.requeue()
        0 * coreV1Api.replaceNamespacedConfigMap(*_)
    }

    void "reconcile of a deleted config map"() {
        given:
        CoreV1Api coreV1Api = Mock()

        when:
        def result = new ConfigMapReconciler(coreV1Api).reconcile(new Request("my-config", "default"), lister(configMap("other")))

        then:
        !result.requeue()
        0 * coreV1Api.replaceNamespacedConfigMap(*_)
    }
}
