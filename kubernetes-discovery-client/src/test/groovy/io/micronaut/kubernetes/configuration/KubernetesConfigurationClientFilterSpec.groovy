package io.micronaut.kubernetes.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.utils.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Requires

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesConfigurationClientFilterSpec extends KubernetesSpecification {

    void setup() {
        KubernetesConfigurationClient.propertySourceCache.clear()
    }

    void "it can filter includes config maps"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.config-maps.includes": "literal-config"], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flux.from(configurationClient.getPropertySources(applicationContext.environment))
                .collectList()
                .block()

        then:
        propertySources.find { it.name.startsWith 'literal-config'}

        and:
        !propertySources.find { it.name.startsWith 'game.json'}
        !propertySources.find { it.name.startsWith 'game.properties'}
        !propertySources.find { it.name.startsWith 'game.yml'}

        cleanup:
        applicationContext.close()
    }

    void "it can filter excludes config maps"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.config-maps.excludes": "literal-config"], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flux.from(configurationClient.getPropertySources(applicationContext.environment))
                .collectList()
                .block()

        then:
        !propertySources.find { it.name.startsWith 'literal-config'}

        and:
        propertySources.find { it.name.startsWith 'game.json'}
        propertySources.find { it.name.startsWith 'game.properties'}
        propertySources.find { it.name.startsWith 'game.yml'}

        cleanup:
        applicationContext.close()
    }

    void "it can have both includes and excludes filters"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": namespace, "kubernetes.client.config-maps.excludes": "literal-config", "kubernetes.client.config-maps.includes": "game-config-yml"], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flux.from(configurationClient.getPropertySources(applicationContext.environment))
                .collectList()
                .block()

        then:
        propertySources
        propertySources.find { it.name.startsWith 'game.yml'}

        and:
        !propertySources.find { it.name.startsWith 'literal-config'}
        !propertySources.find { it.name.startsWith 'game.json'}
        !propertySources.find { it.name.startsWith 'game.properties'}

        cleanup:
        applicationContext.close()
    }
}
