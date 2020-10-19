package io.micronaut.kubernetes.discovery

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.reactivex.Flowable

@Slf4j
class KubernetesDiscoveryClientFilterSpec extends KubernetesSpecification implements KubectlCommands {

    void "it can filter includes services"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": namespace, "kubernetes.client.discovery.includes": "example-client"], Environment.KUBERNETES)
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        when:
        List<String> serviceIds = Flowable.fromPublisher(discoveryClient.getServiceIds()).blockingFirst()

        then:
        serviceIds.size() == 1
        serviceIds.contains("example-client")
//        serviceIds.contains("example-service")
//        serviceIds.contains("example-service-in-other-namespace")

        and:
        Flowable.fromPublisher(discoveryClient.getInstances("example-client")).count().blockingGet() == 1
        Flowable.fromPublisher(discoveryClient.getInstances("example-service")).count().blockingGet() == 0

        cleanup:
        applicationContext.close()
    }

    void "it can filter excludes services"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": namespace, "kubernetes.client.discovery.excludes": "example-client"], Environment.KUBERNETES)
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        when:
        List<String> serviceIds = Flowable.fromPublisher(discoveryClient.getServiceIds()).blockingFirst()

        then:
        serviceIds.size() == 5
        !serviceIds.contains("example-client")
        serviceIds.contains("example-service")
        //serviceIds.contains("example-service-in-other-namespace")

        and:
        Flowable.fromPublisher(discoveryClient.getInstances("example-client")).count().blockingGet() == 0
        Flowable.fromPublisher(discoveryClient.getInstances("example-service")).count().blockingGet() == 1

        cleanup:
        applicationContext.close()
    }
}
