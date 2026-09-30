package io.micronaut.docs.client.rxjava3

//tag::class[]
import io.kubernetes.client.openapi.models.V1PodList
import io.micronaut.kubernetes.client.rxjava3.CoreV1ApiRxClient
import io.reactivex.rxjava3.core.Single
import jakarta.inject.Singleton

@Singleton
class MyService(private val coreV1ApiRxClient: CoreV1ApiRxClient) {

    fun myMethod(namespace: String) {
        val v1PodList: Single<V1PodList> = coreV1ApiRxClient.listNamespacedPod(namespace).execute()
    }
}
//end::class[]
