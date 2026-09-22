package io.micronaut.docs.client.reactor;

//tag::class[]
import io.kubernetes.client.openapi.models.V1PodList;
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

@Singleton
public class MyService {

    private final CoreV1ApiReactorClient coreV1ApiReactorClient;

    public MyService(CoreV1ApiReactorClient coreV1ApiReactorClient) {
        this.coreV1ApiReactorClient = coreV1ApiReactorClient;
    }

    public void myMethod(String namespace) {
        Mono<V1PodList> v1PodList = coreV1ApiReactorClient.listNamespacedPod(namespace).execute();
    }
}
//end::class[]
