package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.v1.KubernetesClient
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class KubernetesDiscoveryClientDisabledSpec extends Specification {
    @Shared
    Map<String, Object> conf = ['kubernetes.discovery-client.enabled': false]

    @AutoCleanup
    @Shared
    ApplicationContext applicationContext = ApplicationContext.run(conf, Environment.KUBERNETES)

    void "KubernetesDiscoveryClientConfiguration exists"() {
        expect:
        applicationContext.containsBean(KubernetesDiscoveryClientConfiguration)

        and:
        applicationContext.containsBean(KubernetesClient)

        and:
        applicationContext.environment.getActiveNames().contains(Environment.KUBERNETES)

        and:
        !applicationContext.containsBean(KubernetesDiscoveryClient)
    }
}
