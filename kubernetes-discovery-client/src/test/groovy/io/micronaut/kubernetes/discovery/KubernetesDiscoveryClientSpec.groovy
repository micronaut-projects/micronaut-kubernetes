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

package io.micronaut.kubernetes.discovery

import io.micronaut.context.env.Environment
import io.micronaut.discovery.ServiceInstance
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Requires
import spock.lang.Specification

import javax.inject.Inject

import static io.micronaut.kubernetes.test.TestUtils.kubernetesApiAvailable
import static io.micronaut.kubernetes.test.TestUtils.serviceExists

@MicronautTest(environments = [Environment.KUBERNETES])
class KubernetesDiscoveryClientSpec extends Specification implements KubectlCommands {

    @Inject
    KubernetesDiscoveryClient discoveryClient

    @Requires({ serviceExists('example-service')})
    void "it can get service instances"() {
        given:
        List<String> ipAddresses = getIps()
        println "Endpoint IP addresses: ${ipAddresses}"

        when:
        List<ServiceInstance> serviceInstances = Flowable.fromPublisher(discoveryClient.getInstances('example-service')).blockingFirst()

        then:
        serviceInstances.size() == ipAddresses.size()
        serviceInstances.every {
            it.port == 8081
            it.metadata.get('foo', String).get() == 'bar'
        }
        ipAddresses.every { String ip ->
            serviceInstances.find { it.host == ip }
        }
    }

    @Requires({ kubernetesApiAvailable()})
    void "it can list all services"() {
        given:
        List<String> allServices = getServices()

        when:
        List<String> serviceIds = Flowable.fromPublisher(discoveryClient.serviceIds).blockingFirst()

        then:
        serviceIds.size() == allServices.size()
        allServices.every { serviceIds.contains it }
    }

    @Requires({ serviceExists('secure-service-port-name') && serviceExists('secure-service-port-number') && serviceExists('secure-service-labels') })
    void "service #serviceId is secure"(String serviceId) {
        when:
        List<ServiceInstance> serviceInstances = Flowable.fromPublisher(discoveryClient.getInstances(serviceId)).blockingFirst()

        then:
        serviceInstances.first().secure

        where:
        serviceId << ['secure-service-port-name', 'secure-service-port-number', 'secure-service-labels']
    }

    @Requires({ serviceExists('non-secure-service') })
    void "non-secure-service is not secure"() {
        when:
        List<ServiceInstance> serviceInstances = Flowable.fromPublisher(discoveryClient.getInstances('non-secure-service')).blockingFirst()

        then:
        !serviceInstances.first().secure
    }
}
