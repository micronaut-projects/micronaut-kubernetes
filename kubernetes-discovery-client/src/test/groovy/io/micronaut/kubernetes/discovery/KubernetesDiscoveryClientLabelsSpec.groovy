package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Requires

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesDiscoveryClientLabelsSpec extends KubernetesSpecification{

    void "it can filter services by labels"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": namespace, "kubernetes.client.discovery.labels": [foo:"bar"]], Environment.KUBERNETES)
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        when:
        List<String> serviceIds = Flowable.fromPublisher(discoveryClient.getServiceIds()).blockingFirst()

        then:
        serviceIds.size() == 1

        and:
        Flowable.fromPublisher(discoveryClient.getInstances("example-client")).blockingFirst().isEmpty()
        Flowable.fromPublisher(discoveryClient.getInstances("example-service")).blockingFirst().size() == 2  // 2 endpoints

        cleanup:
        applicationContext.close()
    }

}
