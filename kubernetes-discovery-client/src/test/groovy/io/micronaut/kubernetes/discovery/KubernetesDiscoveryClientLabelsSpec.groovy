package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Requires

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesDiscoveryClientLabelsSpec extends KubernetesSpecification{

    void "it can filter services by labels"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": namespace, "kubernetes.client.discovery.labels": [foo:"bar"]], Environment.KUBERNETES)
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        when:
        List<String> serviceIds = Flux.from(discoveryClient.getServiceIds()).blockFirst()

        then:
        serviceIds.size() == 1

        and:
        Flux.from(discoveryClient.getInstances("example-client")).blockFirst().isEmpty()
        Flux.from(discoveryClient.getInstances("example-service")).blockFirst().size() == 2  // 2 endpoints

        cleanup:
        applicationContext.close()
    }

}
