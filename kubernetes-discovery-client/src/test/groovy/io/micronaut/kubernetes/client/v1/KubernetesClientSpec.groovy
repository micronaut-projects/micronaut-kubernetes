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
package io.micronaut.kubernetes.client.v1

import io.micronaut.context.env.Environment
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapList
import io.micronaut.kubernetes.client.v1.endpoints.Endpoints
import io.micronaut.kubernetes.client.v1.endpoints.EndpointsList
import io.micronaut.kubernetes.client.v1.pods.Container
import io.micronaut.kubernetes.client.v1.pods.Pod
import io.micronaut.kubernetes.client.v1.pods.PodSpec
import io.micronaut.kubernetes.client.v1.secrets.Secret
import io.micronaut.kubernetes.client.v1.secrets.SecretList
import io.micronaut.kubernetes.client.v1.services.Service
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Requires
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import javax.inject.Inject
import java.util.stream.Collectors
import java.util.stream.IntStream

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesClientSpec extends KubernetesSpecification {

    @Inject
    @Shared
    KubernetesClient client

    void "it can get one service"() {
        when:
        Service service = Flowable.fromPublisher(client.getService(namespace, 'example-service')).blockingFirst()

        then:
        assertThatServiceIsCorrect(service)
    }

    void "it can list endpoints"() {
        when:
        EndpointsList endpointsList = Flowable.fromPublisher(client.listEndpoints(namespace, null)).blockingFirst()

        then:
        endpointsList.items.size() == operations.listEndpoints(namespace).items.size()
    }

    void "it can get one endpoints"() {
        given:
        List<String> ipAddresses = operations.getEndpoints("example-service", namespace)
                .getSubsets()
                .stream()
                .flatMap(s -> s.addresses.stream())
                .map(a -> a.ip).collect(Collectors.toList())

        when:
        Endpoints endpoints = Flowable.fromPublisher(client.getEndpoints(namespace, 'example-service')).blockingFirst()

        then:
        assertThatEndpointsIsCorrect(endpoints, ipAddresses)
    }

    void "it can list config maps"() {
        when:
        ConfigMapList configMapList = Flowable.fromPublisher(client.listConfigMaps(namespace)).blockingFirst()

        then:
        configMapList.items.size() == operations.listConfigMaps(namespace).items.size()
    }

    void "it can create config maps"() {
        given:
        def name = 'created-configmap'
        def data = ['key1': 'value1', 'key2': 'value2']
        ConfigMap body = buildConfigMap(name, data)

        when:
        ConfigMap configMap = Flowable.fromPublisher(client.createConfigMap(namespace, body)).blockingFirst()

        then:
        configMap.metadata.uid != null
        configMap.data == data

        cleanup:
        Flowable.fromPublisher(client.deleteConfigMap(namespace, name)).blockingFirst()
    }

    void "it throws a conflict exception when it create config maps twice"() {
        given:
        def name = 'created-configmap'
        def data = ['key1': 'value1', 'key2': 'value2']
        ConfigMap body = buildConfigMap(name, data)
        Flowable.fromPublisher(client.createConfigMap(namespace, body)).blockingFirst()

        when:
        Flowable.fromPublisher(client.createConfigMap(namespace, body)).blockingFirst()

        then:
        HttpClientResponseException e = thrown(HttpClientResponseException)
        e.getStatus() == HttpStatus.CONFLICT

        cleanup:
        Flowable.fromPublisher(client.deleteConfigMap(namespace, name)).blockingFirst()
    }

    void "it can replace config maps"() {
        given:
        def name = 'replaced-configmap'
        def data = ['key1': 'value1', 'key2': 'value2']
        ConfigMap body = buildConfigMap(name, data)
        Flowable.fromPublisher(client.createConfigMap(namespace, body)).blockingFirst()
        body.data.remove('key1')

        when:
        ConfigMap configMap = Flowable.fromPublisher(client.replaceConfigMap(namespace, name, body)).blockingFirst()

        then:
        configMap.metadata.uid != null
        ! configMap.data.containsKey('key1')
        configMap.data.get('key2') == 'value2'

        cleanup:
        Flowable.fromPublisher(client.deleteConfigMap(namespace, name)).blockingFirst()
    }

    void "it can delete config maps"() {
        given:
        def name = 'created-configmap'
        def data = ['key1': 'value1', 'key2': 'value2']
        ConfigMap body = buildConfigMap(name, data)
        Flowable.fromPublisher(client.createConfigMap(namespace, body)).blockingFirst()

        when:
        Flowable.fromPublisher(client.deleteConfigMap(namespace, name)).blockingFirst()

        then:
        ! Flowable.fromPublisher(client.listConfigMaps(namespace)).blockingFirst().items.collect { it.metadata.name }.contains(name)
    }

    void "it can get one properties config map"() {
        when:
        ConfigMap configMap = Flowable.fromPublisher(client.getConfigMap(namespace, 'game-config-properties')).blockingFirst()

        then:
        configMap.metadata.name == 'game-config-properties'
        configMap.data['game.properties'] == "enemies=zombies\nlives=5\nenemies.cheat=true\nenemies.cheat.level=noGoodRotten\nsecret.code.passphrase=UUDDLRLRBABAS\nsecret.code.allowed=true\nsecret.code.lives=30\n"

    }

    void "it can get one yml config map"() {
        when:
        ConfigMap configMap = Flowable.fromPublisher(client.getConfigMap(namespace, 'game-config-yml')).blockingFirst()

        then:
        configMap.metadata.name == 'game-config-yml'
        configMap.data['game.yml'].contains "enemies.cheat: true"
    }

    void "it can list secrets"() {
        when:
        SecretList secretList = Flowable.fromPublisher(client.listSecrets(namespace)).blockingFirst()

        then:
        secretList.items.find { it.getMetadata().getName().equals("test-secret") }
    }

    void "it can create secrets"() {

        given:
        def name = 'created-secret'
        def data = ['key1': 'value1'.getBytes(), 'key2': 'value2'.getBytes()]
        Secret body = buildSecret(name, data)

        when:
        Secret secret = Flowable.fromPublisher(client.createSecret(namespace, body)).blockingFirst()

        then:
        secret.metadata.uid != null
        secret.data == data

        cleanup:
        Flowable.fromPublisher(client.deleteSecret(namespace, name)).blockingFirst()
    }

    void "it can replace secrets"() {

        given:
        def name = 'replaced-secret'
        def data = ['key1': 'value1'.getBytes(), 'key2': 'value2'.getBytes()]
        def body = buildSecret(name, data)
        Flowable.fromPublisher(client.createSecret(namespace, body)).blockingFirst()
        body.data.remove('key1')

        when:
        Secret secret = Flowable.fromPublisher(client.replaceSecret(namespace, name, body)).blockingFirst()

        then:
        secret.metadata.uid != null
        ! secret.data.containsKey('key1')
        secret.data.containsKey('key2')
        secret.data.get('key2') == 'value2'.getBytes()

        cleanup:
        Flowable.fromPublisher(client.deleteSecret(namespace, name)).blockingFirst()
    }

    void "it can delete secrets"() {
        given:
        def name = 'created-secret'
        def data = ['key1': 'value1'.getBytes(), 'key2': 'value2'.getBytes()]
        Secret body = buildSecret(name, data)
        Flowable.fromPublisher(client.createSecret(namespace, body)).blockingFirst()

        when:
        Flowable.fromPublisher(client.deleteSecret(namespace, name)).blockingFirst()

        then:
        ! Flowable.fromPublisher(client.listSecrets(namespace)).blockingFirst().items.collect { it.metadata.name }.contains(name)
    }

    void "it can get one secret"() {
        when:
        Secret secret = Flowable.fromPublisher(client.getSecret(namespace, 'test-secret')).blockingFirst()

        then:
        secret.getMetadata().getName().equals("test-secret")
    }

    void "it can create pods"() {
        given:
        def name = 'created-pod'
        Pod body = buildPod(name)

        when:
        Pod pod = Flowable.fromPublisher(client.createPod(namespace, body)).blockingFirst()

        then:
        pod.metadata.uid != null
        pod.status.phase in ["Pending"]

        cleanup:
        Flowable.fromPublisher(client.deletePod(namespace, name)).blockingFirst()
    }

    void "it can delete pods"() {
        given:
        def name = 'deleted-pod'
        Pod body = buildPod(name)
        Flowable.fromPublisher(client.createPod(namespace, body)).blockingFirst()
        waitForPodStatus(name, "Succeeded")
        PollingConditions conditions = new PollingConditions(timeout: 30, delay: 2)

        when:
        Flowable.fromPublisher(client.deletePod(namespace, name)).blockingFirst()

        then:
        conditions.eventually {
            ! Flowable.fromPublisher(client.listPods(namespace)).blockingFirst().items.collect { it.metadata.name }.contains(name)
        }
    }

    private boolean assertThatServiceIsCorrect(Service service) {
        service.metadata.name == 'example-service' &&
                service.spec.ports.first().port == 8081 &&
                service.spec.ports.first().targetPort == "8081" &&
                service.spec.clusterIp == InetAddress.getByName(operations.getService(service.metadata.name, service.metadata.namespace).spec.clusterIP)
    }

    private static boolean assertThatEndpointsIsCorrect(Endpoints endpoints, List<String> ipAddresses) {
        endpoints.metadata.name == 'example-service' &&
                endpoints.subsets.first().addresses.first().ip == InetAddress.getByName(ipAddresses.first()) &&
                endpoints.subsets.first().addresses.last().ip == InetAddress.getByName(ipAddresses.last()) &&
                endpoints.subsets.first().ports.first().port == 8081
    }

    private static Secret buildSecret(String name, LinkedHashMap<String, byte[]> data) {
        Secret body = new Secret()
        Metadata metadata = new Metadata()
        metadata.name = name
        body.metadata = metadata
        body.data = data
        body
    }

    private static ConfigMap buildConfigMap(String name, LinkedHashMap<String, String> data) {
        ConfigMap body = new ConfigMap()
        Metadata metadata = new Metadata()
        metadata.name = name
        body.metadata = metadata
        body.data = data
        body
    }

    private static Pod buildPod(String name) {
        Pod body = new Pod()
        Metadata metadata = new Metadata()
        metadata.name = name
        body.metadata = metadata
        PodSpec spec = new PodSpec()
        spec.restartPolicy = 'Never'
        Container container = new Container()
        container.name = 'container'
        container.image = 'alpine:latest'
        container.command = ["echo", "This is not a test!"]
        spec.containers = [container]
        body.spec = spec
        body
    }


    private boolean waitForPodStatus(name, status) {
        IntStream.range(0, 9).any { it ->
            Pod pod = Flowable.fromPublisher(client.getPod(namespace, name)).blockingFirst()
            if (pod.status.phase == status) {
                return true
            } else {
                sleep(1000)
            }
        }
    }
}
