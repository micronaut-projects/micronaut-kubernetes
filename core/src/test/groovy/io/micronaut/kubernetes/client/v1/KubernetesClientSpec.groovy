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

import io.micronaut.kubernetes.client.v1.endpoints.Endpoints
import io.micronaut.kubernetes.client.v1.endpoints.EndpointsList
import io.micronaut.kubernetes.client.v1.services.Service
import io.micronaut.kubernetes.client.v1.services.ServiceList
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest(propertySources = 'classpath:application.yml')
class KubernetesClientSpec extends Specification {

    @Inject
    KubernetesClient client

    void "it can list services"() {
        when:
        ServiceList serviceList = Flowable.fromPublisher(client.listServices()).blockingFirst()

        then:
        serviceList.items.size() == 4
    }

    void "it can get one service"() {
        when:
        Service service = Flowable.fromPublisher(client.getService('default', 'example-service')).blockingFirst()

        then:
        service.metadata.name == 'example-service'
        service.spec.ports.first().port == 8080
        service.spec.ports.first().targetPort == 8080
        service.spec.clusterIp == InetAddress.getByName('10.109.97.98')
    }

    void "it can list endpoints"() {
        when:
        EndpointsList endpointsList = Flowable.fromPublisher(client.listEndpoints()).blockingFirst()

        then:
        endpointsList.items.size() == 6
    }

    void "it can get one endpoints"() {
        when:
        Endpoints endpoints = Flowable.fromPublisher(client.getEndpoints('default', 'example-service')).blockingFirst()

        then:
        endpoints.metadata.name == 'example-service'
        endpoints.subsets.first().addresses.first().ip == InetAddress.getByName('10.1.0.31')
        endpoints.subsets.first().addresses.last().ip == InetAddress.getByName('10.1.0.34')
        endpoints.subsets.first().ports.first().port == 8080
    }

}
