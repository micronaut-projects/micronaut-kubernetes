package io.micronaut.docs.client

import io.kubernetes.client.openapi.ApiClient
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class ApiClientListenerSpec extends Specification {

    @Inject
    ApiClient apiClient

    void "the listener customizes the OkHttp client"() {
        expect:
        apiClient.httpClient.readTimeoutMillis() == 5345
    }
}
