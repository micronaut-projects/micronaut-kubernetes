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
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest(propertySources = 'classpath:application.yml', environments = [Environment.KUBERNETES])
class KubernetesDiscoveryClientSpec extends Specification {

    @Inject
    KubernetesDiscoveryClient discoveryClient

    void "it can get service instances"() {
        given:
        List<String> ipAddresses = getIps()

        when:
        List<ServiceInstance> serviceInstances = Flowable.fromPublisher(discoveryClient.getInstances('example-service')).blockingFirst()

        then:
        serviceInstances.size() == ipAddresses.size()
        serviceInstances.every { it.port == 8081 }
        ipAddresses.every { String ip ->
            serviceInstances.find { it.host == ip }
        }
    }

    void "it can list all services"() {
        given:
        List<String> allServices = getServices()

        when:
        List<String> serviceIds = Flowable.fromPublisher(discoveryClient.serviceIds).blockingFirst()

        then:
        serviceIds.size() == allServices.size()
        allServices.every { serviceIds.contains it }
    }

    private List<String> getServices(){
        return getProcessOutput("kubectl get services --all-namespaces | awk 'FNR > 1 { print \$2 }'").split('\n')
    }

    private List<String> getIps() {
        return getProcessOutput("kubectl get endpoints example-service | awk 'FNR > 1 { print \$2 }'")
                    .split('\\,')
                    .collect { it.split(':').first() }
    }

    private String getProcessOutput(String command) {
        Process p = ['bash', '-c', command].execute()
        p.waitFor()
        return p.text
    }

}
