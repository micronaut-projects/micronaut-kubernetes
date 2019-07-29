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
import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient

trait KubectlCommands {

    @Memoized
    static List<String> getEndpoints(){
        return getProcessOutput("kubectl get endpoints --namespace micronaut-kubernetes | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    @Memoized
    static List<String> getServices(){
        return getProcessOutput("kubectl get services --namespace micronaut-kubernetes | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    @Memoized
    static String getClusterIp() {
        return getProcessOutput("kubectl get service example-service --namespace micronaut-kubernetes | awk 'FNR > 1 { print \$3 }'").trim()
    }

    @Memoized
    static List<String> getIps() {
        return getProcessOutput("kubectl get endpoints example-service --namespace micronaut-kubernetes | awk 'FNR > 1 { print \$2 }'")
                .split('\\,')
                .collect { it.split(':').first() }
    }

    @Memoized
    static List<String> getConfigMaps() {
        return getProcessOutput("kubectl get configmaps --namespace micronaut-kubernetes | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    @Memoized
    static List<String> getSecrets() {
        return getProcessOutput("kubectl get secrets --field-selector type=Opaque --namespace micronaut-kubernetes | awk 'FNR > 1 { print \$1 }'").split('\n')
    }

    static String createConfigMap(String configMapName, Map data = [foo: 'bar']) {
        KubernetesClient client = new DefaultKubernetesClient()
        ObjectMeta objectMeta = new ObjectMeta()
        objectMeta.name = configMapName
        ConfigMap configMap = new ConfigMap()
        configMap.metadata = objectMeta
        configMap.data = data
        ConfigMap result = client.configMaps().inNamespace('micronaut-kubernetes').createOrReplace(configMap)

        println "****"
        println "Result: ${result.toString()}"
        println "****"
        return result.toString()
    }

    static boolean deleteConfigMap(String configMapName) {
        KubernetesClient client = new DefaultKubernetesClient()
        ObjectMeta objectMeta = new ObjectMeta()
        objectMeta.name = configMapName
        ConfigMap configMap = new ConfigMap()
        configMap.metadata = objectMeta
        client.configMaps().inNamespace('micronaut-kubernetes').delete(configMap)
    }

    static String modifyConfigMap(String configMapName) {
        createConfigMap(configMapName, [foo: 'baz'])
    }

    static List<Pod> getPods() {
        KubernetesClient client = new DefaultKubernetesClient()
        return client.pods().inNamespace('micronaut-kubernetes').list().items
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