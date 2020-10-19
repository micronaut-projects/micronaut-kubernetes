package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.v1.KubernetesServiceConfiguration
import spock.lang.Specification

class KubernetesServiceConfigurationSpec extends Specification {

    void "creates service discovery configuration"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                        "kubernetes.client.discovery.services.example-service.namespace": "micronaut-kubernetes",
                        "kubernetes.client.discovery.services.example-service-in-other-namespace.namespace": "micronaut-kubernetes-a",
                        "kubernetes.client.discovery.services.example-service-in-other-namespace.name": "example-service"], Environment.KUBERNETES)

        when:
        Collection<KubernetesServiceConfiguration> discoveryClients = applicationContext.getBeansOfType(KubernetesServiceConfiguration.class)

        then:
        discoveryClients.size() == 2
        discoveryClients.stream()
                .filter(c ->
                        c.getServiceId() == 'example-service' && c.getNamespace().get() == 'micronaut-kubernetes')
                .findFirst().isPresent()
        discoveryClients.stream()
                .filter(c ->
                        c.getServiceId() == 'example-service-in-other-namespace' && c.getNamespace().get() == 'micronaut-kubernetes-a' && c.getName().get() == 'example-service')
                .findFirst().isPresent()

        cleanup:
        applicationContext.close()
    }
}
