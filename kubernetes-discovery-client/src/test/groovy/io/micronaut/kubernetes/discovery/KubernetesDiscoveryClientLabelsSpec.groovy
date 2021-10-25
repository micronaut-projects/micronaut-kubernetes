package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.discovery.provider.KubernetesServiceInstanceEndpointProvider
import io.micronaut.kubernetes.discovery.provider.KubernetesServiceInstanceServiceProvider
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Requires
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesDiscoveryClientLabelsSpec extends KubernetesSpecification {

    @Shared
    PollingConditions pollingConditions = new PollingConditions()

    void "it can filter services by labels for mode #mode with watchEnabled #watchEnabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace"                                          : namespace,
                "kubernetes.client.discovery.labels"                                   : [foo: "bar"],
                "kubernetes.client.discovery.mode"                                     : mode,
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled" : watchEnabled,
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        KubernetesDiscoveryClient discoveryClient = applicationContext.getBean(KubernetesDiscoveryClient)

        when:
        List<String> serviceIds = Flux.from(discoveryClient.getServiceIds()).blockFirst()

        then:
        pollingConditions.eventually {
            serviceIds.size() == 1
            Flux.from(discoveryClient.getInstances("example-client")).blockFirst().isEmpty()
            !Flux.from(discoveryClient.getInstances("example-service")).blockFirst().isEmpty()
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
