package io.micronaut.docs.client

//tag::class[]
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.CoreV1Api
import jakarta.inject.Singleton

@Singleton
class MyService(private val coreV1Api: CoreV1Api) {

    @Throws(ApiException::class)
    fun myMethod(namespace: String) {
        val v1PodList = coreV1Api.listNamespacedPod(namespace).execute()
    }
}
//end::class[]
