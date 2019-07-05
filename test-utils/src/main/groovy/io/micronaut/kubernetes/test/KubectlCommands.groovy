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

import java.util.concurrent.TimeUnit

trait KubectlCommands {

    @Memoized
    static List<String> getEndpoints(){
        return getProcessOutput("kubectl get endpoints | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    @Memoized
    static List<String> getServices(){
        return getProcessOutput("kubectl get services | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    @Memoized
    static String getClusterIp() {
        return getProcessOutput("kubectl get service example-service | awk 'FNR > 1 { print \$3 }'").trim()
    }

    @Memoized
    static List<String> getIps() {
        return getProcessOutput("kubectl get endpoints example-service | awk 'FNR > 1 { print \$2 }'")
                .split('\\,')
                .collect { it.split(':').first() }
    }

    @Memoized
    static List<String> getConfigMaps() {
        return getProcessOutput("kubectl get configmaps | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    @Memoized
    static List<String> getSecrets() {
        return getProcessOutput("kubectl get secrets --field-selector type=Opaque | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    static String createConfigMap(String name) {
        return getProcessOutput("kubectl create configmap ${name} --from-literal=foo=bar")
    }

    static String deleteConfigMap(String name) {
        return getProcessOutput("kubectl delete configmap ${name}")
    }

    static String getProcessOutput(String command) {
        Process p = ['bash', '-c', command].execute()
        String text = p.text

        println "****"
        println "Command: ${command}. Output:\n"
        println text
        println "****"
        return text
    }
}