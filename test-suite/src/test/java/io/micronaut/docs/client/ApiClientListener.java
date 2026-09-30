package io.micronaut.docs.client;

import io.kubernetes.client.openapi.ApiClient;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

//tag::listener[]
@Singleton
public class ApiClientListener implements BeanCreatedEventListener<ApiClient> {

    @Override
    public ApiClient onCreated(BeanCreatedEvent<ApiClient> event) {
        ApiClient apiClient = event.getBean();
        OkHttpClient okHttpClient = apiClient.getHttpClient().newBuilder()
                .readTimeout(5345, TimeUnit.MILLISECONDS)
                .build();
        apiClient.setHttpClient(okHttpClient);
        return apiClient;
    }
}
//end::listener[]
