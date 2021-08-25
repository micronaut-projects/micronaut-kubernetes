package io.micronaut.kubernetes.client

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class DefaultNamespaceResolverSpec extends Specification {

    def "it resolves namespace from property"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": "micronaut",
        ])

        when:
        DefaultNamespaceResolver namespaceResolver = applicationContext.getBean(DefaultNamespaceResolver)

        then:
        namespaceResolver.resolveNamespace() == "micronaut"
    }

    def "it resolves default namespace"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run()

        when:
        DefaultNamespaceResolver namespaceResolver = applicationContext.getBean(DefaultNamespaceResolver)

        then:
        namespaceResolver.resolveNamespace() == DefaultNamespaceResolver.DEFAULT_NAMESPACE
    }
}

