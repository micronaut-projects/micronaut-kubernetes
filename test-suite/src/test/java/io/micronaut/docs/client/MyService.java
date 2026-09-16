package io.micronaut.docs.client;

//tag::class[]
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PodList;
import jakarta.inject.Singleton;

@Singleton
public class MyService {

    private final CoreV1Api coreV1Api;

    public MyService(CoreV1Api coreV1Api) {
        this.coreV1Api = coreV1Api;
    }

    public void myMethod(String namespace) throws ApiException {
        V1PodList v1PodList = coreV1Api.listNamespacedPod(namespace).execute();
    }
}
//end::class[]
