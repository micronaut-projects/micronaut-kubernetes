package io.micronaut.kubernetes

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.KubernetesConfiguration
import io.micronaut.kubernetes.client.DefaultNamespaceResolver
import io.micronaut.kubernetes.test.TestUtils
import spock.lang.Requires
import spock.lang.Specification

@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesConfigurationSpec extends Specification {

    void "the namespace can be set via configuration"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": "other-namespace"], Environment.KUBERNETES)

        when:
        String namespace = applicationContext.getBean(KubernetesConfiguration).namespace

        then:
        namespace == "other-namespace"
    }

    void "when not set, namespace is default"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.builder()
                .deduceEnvironment(false)
                .environments(Environment.KUBERNETES)
                .build()
                .start()

        when:
        String namespace = applicationContext.getBean(KubernetesConfiguration).namespace

        then:
        namespace == DefaultNamespaceResolver.DEFAULT_NAMESPACE;
    }

}
