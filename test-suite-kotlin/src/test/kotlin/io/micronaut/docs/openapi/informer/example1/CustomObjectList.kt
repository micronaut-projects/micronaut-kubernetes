package io.micronaut.docs.openapi.informer.example1

import io.micronaut.kubernetes.client.openapi.common.KubernetesListObject
import io.micronaut.kubernetes.client.openapi.model.V1ListMeta
import io.micronaut.serde.annotation.Serdeable

//tag::get[]
@Serdeable
class CustomObjectList : KubernetesListObject {

    private var apiVersion: String? = null

    private var kind: String? = null

    private var metadata: V1ListMeta = V1ListMeta()

    private var items: List<CustomObject> = emptyList()

    // getters/setters omitted
    //end::get[]

    override fun getApiVersion(): String? = apiVersion

    fun setApiVersion(apiVersion: String?) {
        this.apiVersion = apiVersion
    }

    override fun getKind(): String? = kind

    fun setKind(kind: String?) {
        this.kind = kind
    }

    override fun getMetadata(): V1ListMeta = metadata

    fun setMetadata(metadata: V1ListMeta) {
        this.metadata = metadata
    }

    override fun getItems(): List<CustomObject> = items

    fun setItems(items: List<CustomObject>) {
        this.items = items
    }
//tag::get[]
}
//end::get[]
