package io.micronaut.docs.client.reactor

//tag::class[]
import io.kubernetes.client.openapi.models.V1PodList
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient
import jakarta.inject.Singleton
import reactor.core.publisher.Mono

@Singleton
class MyService(private val coreV1ApiReactorClient: CoreV1ApiReactorClient) {

    fun myMethod(namespace: String) {
        val v1PodList: Mono<V1PodList> = coreV1ApiReactorClient.listNamespacedPod(namespace).execute()
    }
}
//end::class[]
