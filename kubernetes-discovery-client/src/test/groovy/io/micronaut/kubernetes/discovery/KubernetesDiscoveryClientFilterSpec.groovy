package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.discovery.provider.KubernetesServiceInstanceEndpointProvider
import io.micronaut.kubernetes.discovery.provider.KubernetesServiceInstanceServiceProvider
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.utils.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "spec.reuseNamespace", value = "false")
class KubernetesDiscoveryClientFilterSpec extends KubernetesSpecification implements KubectlCommands {

    @Shared
    PollingConditions pollingConditions = new PollingConditions()

    @Unroll
    void "it can filter includes services with watchEnabled #watchEnabled and mode #mode"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace"                                          : namespace,
                "kubernetes.client.discovery.includes"                                 : "example-client",
                "kubernetes.client.discovery.mode"                                     : mode,
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled" : watchEnabled,
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        when:
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        then:
        pollingConditions.eventually {
            List<String> serviceIds = Flux.from(discoveryClient.getServiceIds()).blockFirst()
            serviceIds.size() == 1
            serviceIds.contains("example-client")
            Flux.from(discoveryClient.getInstances("example-client")).blockFirst().size() == 1
            Flux.from(discoveryClient.getInstances("example-service")).blockFirst().isEmpty()
        }

        cleanup:
        applicationContext.close()

        where:
        [mode, watchEnabled] << [
                [KubernetesServiceInstanceServiceProvider.MODE, KubernetesServiceInstanceEndpointProvider.MODE],
                [true, false]
        ].combinations()
    }

    @Unroll
    void "it can filter excludes services with watchEnabled #watchEnabled and mode #mode"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace"                                          : namespace,
                "kubernetes.client.discovery.excludes"                                 : "example-client",
                "kubernetes.client.discovery.mode"                                     : mode,
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled" : watchEnabled,
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        when:
        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        then:
        pollingConditions.eventually {
            List<String> serviceIds = Flux.from(discoveryClient.getServiceIds()).blockFirst()
            serviceIds.size() == 5
            !serviceIds.contains("example-client")
            serviceIds.contains("example-service")
            Flux.from(discoveryClient.getInstances("example-client")).blockFirst().isEmpty()
        }

        cleanup:
        applicationContext.close()

        where:
        [mode, watchEnabled] << [
                [KubernetesServiceInstanceServiceProvider.MODE, KubernetesServiceInstanceEndpointProvider.MODE],
                [true, false]
        ].combinations()
    }
}
