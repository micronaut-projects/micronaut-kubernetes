package io.micronaut.kubernetes.client.openapi.informer.example1;

import io.micronaut.kubernetes.client.openapi.common.KubernetesListObject;
import io.micronaut.kubernetes.client.openapi.model.V1ListMeta;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

//tag::get[]
@Serdeable
public class CustomObjectList implements KubernetesListObject {

    private String apiVersion;

    private String kind;

    private V1ListMeta metadata;

    private List<CustomObject> items;

    // getters/setters omitted
    //end::get[]

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public V1ListMeta getMetadata() {
        return metadata;
    }

    public void setMetadata(V1ListMeta metadata) {
        this.metadata = metadata;
    }

    @Override
    public List<CustomObject> getItems() {
        return items;
    }

    public void setItems(List<CustomObject> items) {
        this.items = items;
    }
//tag::get[]
}
//end::get[]
