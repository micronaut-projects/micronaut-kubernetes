package io.micronaut.kubernetes.configuration

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Requires

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "kubernetes-configuration-client-secret-spec")
@Property(name = "spec.reuseNamespace", value = "false")
@Slf4j
class KubernetesConfigurationClientSecretSpec extends KubernetesSpecification {

    void setup() {
        log.info("Running setup in KubernetesConfigurationClientSecretSpec")
        KubernetesConfigurationClient.propertySourceCache.clear()
    }

    void cleanup() {
        setup()
    }

    void "it can read secrets"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.config-maps.watch": false,
                "kubernetes.client.secrets.enabled": true], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        KubernetesConfigurationClient.propertySourceCache.clear()
        def propertySource = Flux.from(configurationClient.getPropertySources(applicationContext.environment)).
                filter(it -> it.name.startsWith 'test-secret')
                .blockFirst()

        then:
        propertySource.name == 'test-secret (Kubernetes Secret)'
        propertySource.get('username') == 'my-app'
        propertySource.get('password') == '39528$vdg7Jb'

        cleanup:
        applicationContext.close()
    }

    void "it can filter includes secrets"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.config-maps.watch": false,
                "kubernetes.client.secrets.enabled": true,
                "kubernetes.client.secrets.includes": "another-secret"], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        KubernetesConfigurationClient.propertySourceCache.clear()
        def propertySources = Flux.from(configurationClient.getPropertySources(applicationContext.environment))
                .collectList().block()

        then:
        propertySources.find { it.name.startsWith 'another-secret'}

        and:
        !propertySources.find { it.name.startsWith 'test-secret'}

        cleanup:
        applicationContext.close()
    }

    void "it can filter excludes secrets"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.config-maps.watch": false,
                "kubernetes.client.secrets.enabled": true,
                "kubernetes.client.secrets.excludes": "another-secret"], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        KubernetesConfigurationClient.propertySourceCache.clear()
        def propertySources = Flux.from(configurationClient.getPropertySources(applicationContext.environment))
                .collectList().block()

        then:
        propertySources.find { it.name.startsWith 'test-secret'}

        and:
        !propertySources.find { it.name.startsWith 'another-secret'}

        cleanup:
        applicationContext.close()
    }
}
