package io.micronaut.docs.client

import io.kubernetes.client.openapi.ApiClient
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton
import java.util.concurrent.TimeUnit

//tag::listener[]
@Singleton
class ApiClientListener : BeanCreatedEventListener<ApiClient> {

    override fun onCreated(event: BeanCreatedEvent<ApiClient>): ApiClient {
        val apiClient = event.bean
        val okHttpClient = apiClient.httpClient.newBuilder()
            .readTimeout(5345, TimeUnit.MILLISECONDS)
            .build()
        apiClient.setHttpClient(okHttpClient)
        return apiClient
    }
}
//end::listener[]
