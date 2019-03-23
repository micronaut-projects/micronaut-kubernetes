package io.micronaut.kubernetes.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Specification
import spock.lang.Unroll

class KubernetesDiscoveryClientEnabledSpec extends Specification {

    @Unroll("KubernetesDiscoveryConfiguration #description")
    void "KubernetesDiscoveryConfiguration can be disabled through configuration"(Map<String, Object> conf,
                                                                                        boolean beanExists,
                                                                                        String description) {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(conf, Environment.KUBERNETES)

        expect:
        applicationContext.containsBean(KubernetesDiscoveryClient) == beanExists

        cleanup:
        applicationContext.close()

        where:
        conf                                            | beanExists    | description
        [:]                                             | true          | 'bean exists by default'
        ['kubernetes.discovery.enabled': false]         | false         | 'can be disabled with kubernetes.discovery.enabled=false'
    }
}
