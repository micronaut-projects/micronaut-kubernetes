package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.v1.KubernetesClient
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class KubernetesDiscoveryClientEnabledSpec extends Specification {

    @Unroll("KubernetesDiscoveryClientConfiguration #description")
    void "KubernetesDiscoveryClientConfiguration can be disabled through configuration"(Map<String, Object> conf,
                                                                                        boolean beanExists,
                                                                                        String description) {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(conf, Environment.KUBERNETES)

        expect:
        applicationContext.containsBean(KubernetesDiscoveryClient) == beanExists

        cleanup:
        applicationContext.close()

        where:
        conf                                            || beanExists
        [:]                                             || true
        ['kubernetes.discovery-client.enabled': false]  || false
        description = conf == [:] ? 'bean exists by default' : 'can be disabled with kubernetes.discovery-client.enabled=false'
    }
}
