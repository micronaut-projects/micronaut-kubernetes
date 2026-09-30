package io.micronaut.docs.client;

import io.kubernetes.client.openapi.ApiClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class ApiClientListenerTest {

    @Inject
    ApiClient apiClient;

    @Test
    void testTheListenerCustomizesTheOkHttpClient() {
        assertEquals(5345, apiClient.getHttpClient().readTimeoutMillis());
    }
}
