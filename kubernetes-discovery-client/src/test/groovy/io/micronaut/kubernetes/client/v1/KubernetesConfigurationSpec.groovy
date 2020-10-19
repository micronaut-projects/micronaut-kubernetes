package io.micronaut.kubernetes.client.v1

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
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
        ApplicationContext applicationContext = ApplicationContext.build().deduceEnvironment(false).environments(Environment.KUBERNETES).start()

        when:
        String namespace = applicationContext.getBean(KubernetesConfiguration).namespace

        then:
        namespace == KubernetesConfiguration.DEFAULT_NAMESPACE;
    }

}
