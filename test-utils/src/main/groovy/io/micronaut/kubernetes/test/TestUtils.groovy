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
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.util.ClientBuilder

import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

@Slf4j
class TestUtils {

    public static final String KUBEPROXY_BASE_PATH = "http://localhost:8001"

    private static final AtomicBoolean API_CLIENT_FACTORY_INIT = new AtomicBoolean(false)

    @Memoized
    static boolean available(String url) {
        try {
            url.toURL().openConnection().with {
                connectTimeout = 1000
                readTimeout = 1000
                connect()
            }
            log.debug("Kubernetes api available at: {}", url)
            true
        } catch (IOException e) {
            log.error("Kubernetes api is not available at: {}", url, e)
            false
        }
    }

    @Memoized
    static boolean kubernetesApiAvailable() {
        if (available(KUBEPROXY_BASE_PATH)) {
            if (API_CLIENT_FACTORY_INIT.compareAndSet(false, true)) {
                Configuration.setApiClientFactory(new Supplier<ApiClient>() {
                    @Override
                    ApiClient get() {
                        ClientBuilder clientBuilder = new ClientBuilder()
                        clientBuilder.setBasePath(KUBEPROXY_BASE_PATH)
                        return clientBuilder.build()
                    }
                })
            }
            return true
        }
        return false
    }
}
