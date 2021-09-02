package io.micronaut.kubernetes.client

import io.kubernetes.client.openapi.models.V1ClusterRole
import io.kubernetes.client.openapi.models.V1ConfigMap
import spock.lang.Specification

class ModelMapperSpec extends Specification {

    ModelMapper mapper = new ModelMapper()

    def "it resolves core model"() {
        expect:
        with(mapper.getGroupVersionKindByClass(V1ConfigMap)) {
            it.version == "v1"
            it.group == ""
            it.kind == "ConfigMap"
        }
    }

    def "it resolves rbac model"() {
        expect:
        with(mapper.getGroupVersionKindByClass(V1ClusterRole)) {
            it.version == "v1"
            it.group == ""
            it.kind == "ClusterRole"
        }
    }
}
