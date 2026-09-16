package micronaut.client.configmap

import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.informer.cache.Indexer
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver
import spock.lang.Specification

class ConfigMapControllerSpec extends Specification {

    private static V1ConfigMap configMap(String name) {
        new V1ConfigMap().metadata(new V1ObjectMeta().name(name).namespace("default"))
    }

    private ConfigMapController controller(V1ConfigMap... configMaps) {
        Indexer<V1ConfigMap> indexer = Stub(Indexer) {
            list() >> configMaps.toList()
            getByKey(_) >> { String key -> configMaps.find { "default/" + it.metadata.name == key } }
        }
        SharedIndexInformer<V1ConfigMap> informer = Stub(SharedIndexInformer) {
            getIndexer() >> indexer
        }
        SharedIndexInformerFactory factory = Stub(SharedIndexInformerFactory) {
            getExistingSharedIndexInformer(V1ConfigMap, "default") >> informer
        }
        NamespaceResolver namespaceResolver = Stub(NamespaceResolver) {
            resolveNamespace() >> "default"
        }
        new ConfigMapController(factory, namespaceResolver)
    }

    void "all returns the cached config maps"() {
        given:
        ConfigMapController controller = controller(configMap("first"), configMap("second"))

        expect:
        controller.all()*.metadata*.name == ["first", "second"]
    }

    void "a config map is looked up by key"() {
        given:
        ConfigMapController controller = controller(configMap("first"))

        expect:
        controller.configMap("first").metadata.name == "first"
        controller.configMap("missing") == null
    }
}
