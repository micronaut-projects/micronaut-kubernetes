package io.micronaut.docs.openapi.informer.example1

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.serde.annotation.Serdeable

//tag::get[]
@Serdeable
class CustomObject implements KubernetesObject {

    private String apiVersion

    private String kind

    private V1ObjectMeta metadata

    private String value

    // getters/setters omitted
    //end::get[]

    @Override
    String getApiVersion() {
        return apiVersion
    }

    void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion
    }

    @Override
    String getKind() {
        return kind
    }

    void setKind(String kind) {
        this.kind = kind
    }

    @Override
    V1ObjectMeta getMetadata() {
        return metadata
    }

    void setMetadata(V1ObjectMeta metadata) {
        this.metadata = metadata
    }

    String getValue() {
        return value
    }

    void setValue(String value) {
        this.value = value
    }
//tag::get[]
}
//end::get[]
