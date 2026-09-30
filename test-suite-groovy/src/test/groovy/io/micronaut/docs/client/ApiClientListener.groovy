package io.micronaut.docs.client

import io.kubernetes.client.openapi.ApiClient
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton
import okhttp3.OkHttpClient

import java.util.concurrent.TimeUnit

//tag::listener[]
@Singleton
class ApiClientListener implements BeanCreatedEventListener<ApiClient> {

    @Override
    ApiClient onCreated(BeanCreatedEvent<ApiClient> event) {
        ApiClient apiClient = event.bean
        OkHttpClient okHttpClient = apiClient.httpClient.newBuilder()
                .readTimeout(5345, TimeUnit.MILLISECONDS)
                .build()
        apiClient.httpClient = okHttpClient
        return apiClient
    }
}
//end::listener[]
