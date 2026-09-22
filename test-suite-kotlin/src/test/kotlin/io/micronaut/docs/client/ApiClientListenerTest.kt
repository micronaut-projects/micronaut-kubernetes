package io.micronaut.docs.client

import io.kubernetes.client.openapi.ApiClient
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest
class ApiClientListenerTest {

    @Inject
    lateinit var apiClient: ApiClient

    @Test
    fun testTheListenerCustomizesTheOkHttpClient() {
        assertEquals(5345, apiClient.httpClient.readTimeoutMillis)
    }
}
