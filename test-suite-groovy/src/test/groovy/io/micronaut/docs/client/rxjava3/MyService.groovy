package io.micronaut.docs.client.rxjava3

//tag::class[]
import io.kubernetes.client.openapi.models.V1PodList
import io.micronaut.kubernetes.client.rxjava3.CoreV1ApiRxClient
import io.reactivex.rxjava3.core.Single
import jakarta.inject.Singleton

@Singleton
class MyService {

    private final CoreV1ApiRxClient coreV1ApiRxClient

    MyService(CoreV1ApiRxClient coreV1ApiRxClient) {
        this.coreV1ApiRxClient = coreV1ApiRxClient
    }

    void myMethod(String namespace) {
        Single<V1PodList> v1PodList = coreV1ApiRxClient.listNamespacedPod(namespace).execute()
    }
}
//end::class[]
