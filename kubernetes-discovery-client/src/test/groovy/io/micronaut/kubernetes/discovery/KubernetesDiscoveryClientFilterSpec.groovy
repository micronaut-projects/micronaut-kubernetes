package io.micronaut.kubernetes.discovery

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubectlCommands
import io.reactivex.Flowable
import spock.lang.Requires
import spock.lang.Specification

import static io.micronaut.kubernetes.test.TestUtils.kubernetesApiAvailable

@Slf4j
class KubernetesDiscoveryClientFilterSpec extends Specification implements KubectlCommands {

    @Requires({ kubernetesApiAvailable() && KubernetesDiscoveryClientFilterSpec.getServices().size() })
    void "it can filter includes services"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.discovery.includes": "example-client"], Environment.KUBERNETES)
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        when:
        List<String> serviceIds = Flowable.fromPublisher(discoveryClient.getServiceIds()).blockingFirst()

        then:
        serviceIds.size() == 3
        serviceIds.contains("example-client")
        serviceIds.contains("example-service")
        serviceIds.contains("example-service-in-other-namespace")

        and:
        Flowable.fromPublisher(discoveryClient.getInstances("example-client")).count().blockingGet() == 1
        Flowable.fromPublisher(discoveryClient.getInstances("example-service")).count().blockingGet() == 0

        cleanup:
        applicationContext.close()
    }

    @Requires({ kubernetesApiAvailable() && KubernetesDiscoveryClientFilterSpec.getServices().size() })
    void "it can filter excludes services"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.discovery.excludes": "example-client"], Environment.KUBERNETES)
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        when:
        List<String> serviceIds = Flowable.fromPublisher(discoveryClient.getServiceIds()).blockingFirst()

        then:
        serviceIds.size() == 6
        !serviceIds.contains("example-client")
        serviceIds.contains("example-service")
        serviceIds.contains("example-service-in-other-namespace")

        and:
        Flowable.fromPublisher(discoveryClient.getInstances("example-client")).count().blockingGet() == 0
        Flowable.fromPublisher(discoveryClient.getInstances("example-service")).count().blockingGet() == 1


        cleanup:
        applicationContext.close()
    }
}
