package io.micronaut.kubernetes.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.exceptions.NoSuchBeanException
import spock.lang.Issue
import spock.lang.Specification

class DefaultNamespaceResolverSpec extends Specification {

    def "it resolves namespace from property"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": "micronaut",
        ], Environment.KUBERNETES)

        when:
        DefaultNamespaceResolver namespaceResolver = applicationContext.getBean(DefaultNamespaceResolver)

        then:
        namespaceResolver.resolveNamespace() == "micronaut"

        cleanup:
        applicationContext.close()
    }

    def "it resolves default namespace"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(Environment.KUBERNETES)

        when:
        DefaultNamespaceResolver namespaceResolver = applicationContext.getBean(DefaultNamespaceResolver)

        then:
        namespaceResolver.resolveNamespace() == DefaultNamespaceResolver.DEFAULT_NAMESPACE

        cleanup:
        applicationContext.close()
    }

    @Issue("https://github.com/micronaut-projects/micronaut-kubernetes/issues/473")
    void "when the kubernetes environment is not active the bean is not loaded"() {
        given:
        ApplicationContext ctx = ApplicationContext.run()

        when:
        ctx.getBean(NamespaceResolver)

        then:
        thrown(NoSuchBeanException)

        cleanup:
        ctx.close()
    }
}

