/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.test

import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException

@Slf4j
class TestUtils {

    @Memoized
    static boolean available(String url) {
        log.debug("Determining whether the URL [{}] is available", url)
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
    static boolean serviceExists(String servicesUrl = "http://localhost:8001", String uri = "/api/v1/namespaces/default/services", String serviceName) {
        log.debug("Determining whether the Kubernetes service [{}] is available", serviceName)
        try {
            if (kubernetesApiAvailable()) {
                Map payload = HttpClient.create(new URL(servicesUrl))
                        .toBlocking()
                        .exchange(HttpRequest.GET(uri), Map)
                        .body()
                payload["items"].find { it.metadata.name == serviceName }
            } else {
                return false
            }
        } catch(HttpClientResponseException e) {
            return false
        }
    }

    @Memoized
    static boolean configMapExists(String servicesUrl = "http://localhost:8001", String uri = "/api/v1/namespaces/default/configmaps", String configMapName) {
        log.debug("Determining whether the Kubernetes config map [{}] is available", configMapName)
        try {
            if (kubernetesApiAvailable()) {
                Map payload = HttpClient.create(new URL(servicesUrl))
                        .toBlocking()
                        .exchange(HttpRequest.GET(uri), Map)
                        .body()
                payload["items"].find { it.metadata.name == configMapName }
            } else {
                return false
            }
        } catch(HttpClientResponseException e) {
            return false
        }
    }

    @Memoized
    static boolean secretExists(String servicesUrl = "http://localhost:8001", String uri = "/api/v1/namespaces/default/secrets", String secretName) {
        log.debug("Determining whether the Kubernetes secret [{}] is available", secretName)
        try {
            if (kubernetesApiAvailable()) {
                Map payload = HttpClient.create(new URL(servicesUrl))
                        .toBlocking()
                        .exchange(HttpRequest.GET(uri), Map)
                        .body()
                payload["items"].find { it.metadata.name == secretName }
            } else {
                return false
            }
        } catch(HttpClientResponseException e) {
            return false
        }
    }

    static boolean kubernetesApiAvailable() {
        available("http://localhost:8001")
    }
}
