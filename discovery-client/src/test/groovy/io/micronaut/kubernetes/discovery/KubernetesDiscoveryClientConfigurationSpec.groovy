package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class KubernetesDiscoveryClientConfigurationSpec extends Specification {

    @AutoCleanup
    @Shared
    ApplicationContext applicationContext = ApplicationContext.run()

    void "KubernetesDiscoveryClientConfiguration exists"() {
        expect:
        applicationContext.containsBean(KubernetesDiscoveryClientConfiguration)
    }
}
