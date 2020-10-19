package io.micronaut.kubernetes.configuration

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.reactivex.Flowable

@Slf4j
class KubernetesConfigurationClientLabelsSpec extends KubernetesSpecification implements KubectlCommands {

    void setup() {
        KubernetesConfigurationClient.propertySourceCache.clear()
    }

    void "it can filter config maps by labels"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": namespace, "kubernetes.client.config-maps.labels": [app:"game"]], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()

        then:
        propertySources.find { it.name.startsWith 'literal-config'}
        propertySources.find { it.name.startsWith 'game.yml'}

        and:
        !propertySources.find { it.name.startsWith 'game.json'}
        !propertySources.find { it.name.startsWith 'game.properties'}

        cleanup:
        applicationContext.close()
    }

    void "non-matching labels don't produce property sources"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": namespace, "kubernetes.client.config-maps.labels": [foo:"bar"]], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()

        then:
        propertySources.size() == 0
    }

    void "it can filter secrets by labels"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": namespace, "kubernetes.client.secrets.enabled": true, "kubernetes.client.secrets.labels": [app:"game"]], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()

        then:
        propertySources.find { it.name.startsWith 'another-secret' }

        and:
        !propertySources.find { it.name.startsWith 'test-secret' }

        cleanup:
        applicationContext.close()
    }
}
