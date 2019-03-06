package io.micronaut.kubernetes.test

import groovy.transform.Memoized
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException

class TestUtils {

    @Memoized
    static boolean available(String url) {
        try {
            url.toURL().openConnection().with {
                connectTimeout = 1000
                readTimeout = 1000
                connect()
            }
            true
        } catch (IOException e) {
            false
        }
    }

    @Memoized
    static boolean serviceExists(String servicesUrl, String uri, String serviceName) {
        try {
            Map payload = HttpClient.create(new URL(servicesUrl))
                    .toBlocking()
                    .exchange(HttpRequest.GET(uri), Map)
                    .body()
            payload["items"].find { it.metadata.name == serviceName }
        } catch(HttpClientResponseException e) {
            return false
        }
    }
}
