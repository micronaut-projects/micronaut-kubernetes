/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.kubernetes.openapi.test

import groovy.transform.Memoized
import groovy.util.logging.Slf4j

@Slf4j
class TestUtils {

    private static final String KUBE_PROXY_BASE_PATH = "http://localhost:8001"

    public static final Map KUBE_PROXY_CONFIG_MAP = [
            'current-context': 'test-context',
            'contexts': [
                    ['name': 'test-context', 'context': [cluster: 'test-cluster', user: 'test-user']]
            ],
            'clusters': [
                    ['name': 'test-cluster', cluster: ['server': KUBE_PROXY_BASE_PATH]]
            ],
            users: [
                    [name: 'test-user', user: ['token': 'test-token']]
            ]
    ]

    @Memoized
    static boolean kubernetesApiAvailable() {
        try {
            KUBE_PROXY_BASE_PATH.toURL().openConnection().with {
                connectTimeout = 1000
                readTimeout = 1000
                connect()
            }
            log.debug("Kubernetes api available at: {}", KUBE_PROXY_BASE_PATH)
            true
        } catch (IOException e) {
            log.error("Kubernetes api is not available at: {}", KUBE_PROXY_BASE_PATH, e)
            false
        }
    }
}
