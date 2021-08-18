package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
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
        List<String> serviceIds = Flux.from(discoveryClient.getServiceIds()).blockFirst()

        then:
        serviceIds.size() == 1
        serviceIds.contains("example-client")

        and:
        Flux.from(discoveryClient.getInstances("example-client")).blockFirst().size() == 1
        Flux.from(discoveryClient.getInstances("example-service")).blockFirst().isEmpty()

        cleanup:
        applicationContext.close()
    }

    void "it can filter excludes services"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": namespace, "kubernetes.client.discovery.excludes": "example-client"], Environment.KUBERNETES)
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        when:
        List<String> serviceIds = Flux.from(discoveryClient.getServiceIds()).blockFirst()

        then:
        serviceIds.size() == 5
        !serviceIds.contains("example-client")
        serviceIds.contains("example-service")

        and:
        Flux.from(discoveryClient.getInstances("example-client")).blockFirst().isEmpty()
        Flux.from(discoveryClient.getInstances("example-service")).blockFirst().size() == 2  // 2 endpoints

        cleanup:
        applicationContext.close()
    }
}
