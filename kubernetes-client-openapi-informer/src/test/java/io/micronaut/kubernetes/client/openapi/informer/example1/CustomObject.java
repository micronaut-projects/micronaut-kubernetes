package io.micronaut.kubernetes.client.openapi.informer.example1;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import io.micronaut.serde.annotation.Serdeable;

//tag::get[]
@Serdeable
public class CustomObject implements KubernetesObject {

    private String apiVersion;

    private String kind;

    private V1ObjectMeta metadata;

    private String value;

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
    public V1ObjectMeta getMetadata() {
        return metadata;
    }

    public void setMetadata(V1ObjectMeta metadata) {
        this.metadata = metadata;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
//tag::get[]
}
//end::get[]
