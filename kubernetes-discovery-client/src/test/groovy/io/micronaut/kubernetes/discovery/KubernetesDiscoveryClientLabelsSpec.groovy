package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.reactivex.Flowable

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
        Flowable.fromPublisher(discoveryClient.getInstances("example-client")).count().blockingGet() == 0
        Flowable.fromPublisher(discoveryClient.getInstances("example-service")).count().blockingGet() == 1

        cleanup:
        applicationContext.close()
    }

}
