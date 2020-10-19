package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.configuration.KubernetesConfigurationClientFilterSpec
import io.micronaut.kubernetes.test.KubectlCommands
import io.reactivex.Flowable
import spock.lang.Requires
import spock.lang.Specification

import static io.micronaut.kubernetes.test.TestUtils.kubernetesApiAvailable

class KubernetesDiscoveryClientLabelsSpec extends Specification implements KubectlCommands{

    @Requires({ kubernetesApiAvailable() && KubernetesConfigurationClientFilterSpec.getServices().size() })
    void "it can filter services by labels"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.discovery.labels": [foo:"bar"]], Environment.KUBERNETES)
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        when:
        List<String> serviceIds = Flowable.fromPublisher(discoveryClient.getServiceIds()).blockingFirst()

        then:
        serviceIds.size() == 2
        serviceIds.contains("example-service")
        serviceIds.contains("example-service-in-other-namespace")

        and:
        Flowable.fromPublisher(discoveryClient.getInstances("example-client")).count().blockingGet() == 0
        Flowable.fromPublisher(discoveryClient.getInstances("example-service")).count().blockingGet() == 1

        cleanup:
        applicationContext.close()
    }

}
