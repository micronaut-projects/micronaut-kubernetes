package io.micronaut.kubernetes.client.openapi.resolver

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class DefaultNamespaceResolverSpec extends Specification {

    @Shared
    Path namespaceDir = Files.createTempDirectory("namespace-temp-")

    @Shared
    Path namespaceFile = namespaceDir.resolve("namespace")

    def setup() {
        namespaceFile.toFile().text = 'namespace2'
    }

    def cleanupSpec() {
        if (namespaceFile != null) {
            Files.deleteIfExists(namespaceFile)
        }
        if (namespaceDir) {
            Files.deleteIfExists(namespaceDir)
        }
    }

    def 'resolve namespace from properties'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.namespace': 'namespace1'
        ], Environment.KUBERNETES)
        NamespaceResolver resolver = context.getBean(NamespaceResolver.class)

        when:
        String namespace = resolver.resolveNamespace()

        then:
        namespace == 'namespace1'

        cleanup:
        context.close()
    }

    def 'resolve namespace from files'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.service-account.namespace-path': 'file:' + namespaceFile
        ], Environment.KUBERNETES)
        NamespaceResolver resolver = context.getBean(NamespaceResolver.class)

        when:
        String namespace = resolver.resolveNamespace()

        then:
        namespace == 'namespace2'

        cleanup:
        context.close()
    }

    def 'resolve namespace file not found'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'kubernetes.client.service-account.namespace-path': 'file:' + namespaceFile + '_invalid'
        ], Environment.KUBERNETES)
        NamespaceResolver resolver = context.getBean(NamespaceResolver.class)

        when:
        String namespace = resolver.resolveNamespace()

        then:
        namespace == 'default'

        cleanup:
        context.close()
    }
}
