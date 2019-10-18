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

import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapList
import io.micronaut.kubernetes.client.v1.endpoints.Endpoints
import io.micronaut.kubernetes.client.v1.endpoints.EndpointsList
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

@MicronautTest(environments = [Environment.KUBERNETES])
@Property(name = "kubernetes.client.namespace", value = "micronaut-kubernetes")
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

    @Requires({ TestUtils.configMapExists('game-config-properties')})
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

    @Requires({ TestUtils.secretExists('test-secret')})
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
}
