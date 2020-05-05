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

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
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
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Requires
import spock.lang.Specification

import javax.inject.Inject
import java.util.stream.IntStream

@MicronautTest(environments = [Environment.KUBERNETES])
@Property(name = "kubernetes.client.namespace", value = "micronaut-kubernetes")
@Slf4j
class KubernetesClientSpec extends Specification implements KubectlCommands {

    public static final String DEFAULT_NAMESPACE = 'micronaut-kubernetes'

    @Inject
    KubernetesClient client

    @Requires({ TestUtils.serviceExists('example-service')})
    void "it can get one service"() {
        when:
        Service service = Flowable.fromPublisher(client.getService(DEFAULT_NAMESPACE, 'example-service')).blockingFirst()

        then:
        assertThatServiceIsCorrect(service)
    }

    @Requires({ TestUtils.kubernetesApiAvailable()})
    void "it can list endpoints"() {
        when:
        EndpointsList endpointsList = Flowable.fromPublisher(client.listEndpoints(DEFAULT_NAMESPACE, null)).blockingFirst()

        then:
        endpointsList.items.size() == getEndpoints().size()
    }

    @Requires({ TestUtils.serviceExists('example-service')})
    void "it can get one endpoints"() {
        given:
        List<String> ipAddresses = getIps()

        when:
        Endpoints endpoints = Flowable.fromPublisher(client.getEndpoints(DEFAULT_NAMESPACE, 'example-service')).blockingFirst()

        then:
        assertThatEndpointsIsCorrect(endpoints, ipAddresses)
    }

    @Requires({ TestUtils.kubernetesApiAvailable()})
    void "it can list config maps"() {
        when:
        ConfigMapList configMapList = Flowable.fromPublisher(client.listConfigMaps(DEFAULT_NAMESPACE)).blockingFirst()

        then:
        configMapList.items.size() == configMaps.size()
    }

    @Requires({ TestUtils.kubernetesApiAvailable() })
    void "it can create config maps"() {
        given:

        def name = 'created-configmap'
        def data = ['key1': 'value1', 'key2': 'value2']

        ConfigMap body = buildConfigMap(name, data)

        when:
        ConfigMap configMap = Flowable.fromPublisher(client.createConfigMap(DEFAULT_NAMESPACE, body)).blockingFirst()

        then:
        configMap.metadata.uid != null
        configMap.data == data

        cleanup:
        Flowable.fromPublisher(client.deleteConfigMap(DEFAULT_NAMESPACE, name)).blockingFirst()
    }

    @Requires({ TestUtils.kubernetesApiAvailable() })
    void "it can delete config maps"() {
        given:
        def name = 'created-configmap'
        def data = ['key1': 'value1', 'key2': 'value2']
        ConfigMap body = buildConfigMap(name, data)
        Flowable.fromPublisher(client.createConfigMap(DEFAULT_NAMESPACE, body)).blockingFirst()

        when:
        Flowable.fromPublisher(client.deleteConfigMap(DEFAULT_NAMESPACE, name)).blockingFirst()

        then:
        ! Flowable.fromPublisher(client.listConfigMaps(DEFAULT_NAMESPACE)).blockingFirst().items.collect { it.metadata.name }.contains(name)
    }

    @Requires({ TestUtils.configMapExists('game-config-properties') })
    void "it can get one properties config map"() {
        when:
        ConfigMap configMap = Flowable.fromPublisher(client.getConfigMap(DEFAULT_NAMESPACE, 'game-config-properties')).blockingFirst()

        then:
        configMap.metadata.name == 'game-config-properties'
        configMap.data['game.properties'] == "enemies=zombies\nlives=5\nenemies.cheat=true\nenemies.cheat.level=noGoodRotten\nsecret.code.passphrase=UUDDLRLRBABAS\nsecret.code.allowed=true\nsecret.code.lives=30"

    }

    @Requires({ TestUtils.configMapExists('game-config-yml')})
    void "it can get one yml config map"() {
        when:
        ConfigMap configMap = Flowable.fromPublisher(client.getConfigMap(DEFAULT_NAMESPACE, 'game-config-yml')).blockingFirst()

        then:
        configMap.metadata.name == 'game-config-yml'
        configMap.data['game.yml'].contains "enemies.cheat: true"
    }

    @Requires({ TestUtils.secretExists('test-secret')})
    void "it can list secrets"() {
        when:
        SecretList secretList = Flowable.fromPublisher(client.listSecrets(DEFAULT_NAMESPACE)).blockingFirst()

        then:
        secretList.items.find { it.getMetadata().getName().equals("test-secret") }
    }

    @Requires({ TestUtils.kubernetesApiAvailable() })
    void "it can create secrets"() {

        given:
        def name = 'created-secret'
        def data = ['key1': 'value1'.getBytes(), 'key2': 'value2'.getBytes()]
        Secret body = buildSecret(name, data)

        when:
        Secret secret = Flowable.fromPublisher(client.createSecret(DEFAULT_NAMESPACE, body)).blockingFirst()

        then:
        secret.metadata.uid != null
        secret.data == data

        cleanup:
        Flowable.fromPublisher(client.deleteSecret(DEFAULT_NAMESPACE, name)).blockingFirst()
    }

    @Requires({ TestUtils.kubernetesApiAvailable() })
    void "it can delete secrets"() {
        given:
        def name = 'created-secret'
        def data = ['key1': 'value1'.getBytes(), 'key2': 'value2'.getBytes()]
        Secret body = buildSecret(name, data)
        Flowable.fromPublisher(client.createSecret(DEFAULT_NAMESPACE, body)).blockingFirst()

        when:
        Flowable.fromPublisher(client.deleteSecret(DEFAULT_NAMESPACE, name)).blockingFirst()

        then:
        ! Flowable.fromPublisher(client.listSecrets(DEFAULT_NAMESPACE)).blockingFirst().items.collect { it.metadata.name }.contains(name)
    }

    @Requires({ TestUtils.secretExists('test-secret') })
    void "it can get one secret"() {
        when:
        Secret secret = Flowable.fromPublisher(client.getSecret(DEFAULT_NAMESPACE, 'test-secret')).blockingFirst()

        then:
        secret.getMetadata().getName().equals("test-secret")
    }

    private boolean assertThatServiceIsCorrect(Service service) {
        service.metadata.name == 'example-service' &&
                service.spec.ports.first().port == 8081 &&
                service.spec.ports.first().targetPort == "8081" &&
                service.spec.clusterIp == InetAddress.getByName(getClusterIp())
    }

    private boolean assertThatEndpointsIsCorrect(Endpoints endpoints, List<String> ipAddresses) {
        endpoints.metadata.name == 'example-service' &&
                endpoints.subsets.first().addresses.first().ip == InetAddress.getByName(ipAddresses.first()) &&
                endpoints.subsets.first().addresses.last().ip == InetAddress.getByName(ipAddresses.last()) &&
                endpoints.subsets.first().ports.first().port == 8081
    }

    private Secret buildSecret(String name, LinkedHashMap<String, byte[]> data) {
        Secret body = new Secret()
        Metadata metadata = new Metadata()
        metadata.name = name
        body.metadata = metadata
        body.data = data
        body
    }

    private ConfigMap buildConfigMap(String name, LinkedHashMap<String, String> data) {
        ConfigMap body = new ConfigMap()
        Metadata metadata = new Metadata()
        metadata.name = name
        body.metadata = metadata
        body.data = data
        body
    }

}
