package io.micronaut.docs.openapi.informer.example1

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.serde.annotation.Serdeable

//tag::get[]
@Serdeable
class CustomObject : KubernetesObject {

    private var apiVersion: String? = null

    private var kind: String? = null

    private var metadata: V1ObjectMeta? = null

    var value: String? = null

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

    override fun getMetadata(): V1ObjectMeta? = metadata

    fun setMetadata(metadata: V1ObjectMeta?) {
        this.metadata = metadata
    }
//tag::get[]
}
//end::get[]
