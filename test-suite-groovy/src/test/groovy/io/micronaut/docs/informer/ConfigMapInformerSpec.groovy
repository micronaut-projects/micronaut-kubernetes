package io.micronaut.docs.informer

import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ObjectMeta
import spock.lang.Specification

class ConfigMapInformerSpec extends Specification {

    private static V1ConfigMap configMap(String name) {
        new V1ConfigMap().metadata(new V1ObjectMeta().name(name).namespace("default"))
    }

    void "the handler records the events"() {
        given:
        ConfigMapInformer informer = new ConfigMapInformer()

        when:
        informer.onAdd(configMap("added"))
        informer.onUpdate(configMap("added"), configMap("updated"))
        informer.onDelete(configMap("deleted"), false)

        then:
        informer.added*.metadata*.name == ["added"]
        informer.updated*.metadata*.name == ["updated"]
        informer.deleted*.metadata*.name == ["deleted"]
    }
}
