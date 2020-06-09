/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
class TestUtils implements KubectlCommands {

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
    static boolean serviceExists(String serviceName) {
        kubernetesApiAvailable() && getServices().contains(serviceName)
    }

    @Memoized
    static boolean configMapExists(String configMapName) {
        kubernetesApiAvailable() && getConfigMaps().contains(configMapName)
    }

    @Memoized
    static boolean secretExists(String secretName) {
        kubernetesApiAvailable() && getSecrets().contains(secretName)
    }

    @Memoized
    static boolean kubernetesApiAvailable() {
        available("http://localhost:8001")
    }
}
