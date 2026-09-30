package io.micronaut.docs.openapi.informer.example1

import io.micronaut.kubernetes.client.openapi.common.KubernetesListObject
import io.micronaut.kubernetes.client.openapi.model.V1ListMeta
import io.micronaut.serde.annotation.Serdeable

//tag::get[]
@Serdeable
class CustomObjectList implements KubernetesListObject {

    private String apiVersion

    private String kind

    private V1ListMeta metadata

    private List<CustomObject> items

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
    V1ListMeta getMetadata() {
        return metadata
    }

    void setMetadata(V1ListMeta metadata) {
        this.metadata = metadata
    }

    @Override
    List<CustomObject> getItems() {
        return items
    }

    void setItems(List<CustomObject> items) {
        this.items = items
    }
//tag::get[]
}
//end::get[]
