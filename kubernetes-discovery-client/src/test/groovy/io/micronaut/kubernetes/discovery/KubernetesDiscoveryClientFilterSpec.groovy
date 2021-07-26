package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.discovery.ServiceInstance
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Requires

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "spec.reuseNamespace", value = "false")
class KubernetesDiscoveryClientFilterSpec extends KubernetesSpecification implements KubectlCommands {

    void "it can filter includes services"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.discovery.includes": "example-client"], Environment.KUBERNETES)
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        when:
        List<String> serviceIds = Flowable.fromPublisher(discoveryClient.getServiceIds()).blockingFirst()

        then:
        serviceIds.size() == 1
        serviceIds.contains("example-client")

        and:
        Flowable.fromPublisher(discoveryClient.getInstances("example-client")).blockingFirst().size() == 1
        Flowable.fromPublisher(discoveryClient.getInstances("example-service")).blockingFirst().isEmpty()

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

        and:
        Flowable.fromPublisher(discoveryClient.getInstances("example-client")).blockingFirst().isEmpty()
        Flowable.fromPublisher(discoveryClient.getInstances("example-service")).blockingFirst().size() == 2  // 2 endpoints

        cleanup:
        applicationContext.close()
    }
}
