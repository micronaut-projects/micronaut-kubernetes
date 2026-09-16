package io.micronaut.docs.operator

import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ObjectMeta
import spock.lang.Specification

class OperatorFiltersSpec extends Specification {

    private static V1ConfigMap configMap(Map<String, String> annotations) {
        new V1ConfigMap().metadata(new V1ObjectMeta().name("test").namespace("default").annotations(annotations))
    }

    void "onAddFilter selects annotated config maps"() {
        expect:
        new OnAddFilter().test(configMap(["io.micronaut.operator": "processed"]))
        !new OnAddFilter().test(configMap(["other": "value"]))
        !new OnAddFilter().test(configMap(null))
    }

    void "onUpdateFilter selects config maps whose new version is annotated"() {
        expect:
        new OnUpdateFilter().test(configMap(null), configMap(["io.micronaut.operator": "processed"]))
        !new OnUpdateFilter().test(configMap(["io.micronaut.operator": "processed"]), configMap(null))
    }

    void "onDeleteFilter selects annotated config maps"() {
        expect:
        new OnDeleteFilter().test(configMap(["io.micronaut.operator": "processed"]), false)
        !new OnDeleteFilter().test(configMap(null), true)
    }
}
