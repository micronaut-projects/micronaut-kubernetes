package io.micronaut.kubernetes.configuration

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubectlCommands
import io.reactivex.Flowable
import spock.lang.Requires
import spock.lang.Specification

import static io.micronaut.kubernetes.test.TestUtils.kubernetesApiAvailable

@Slf4j
class KubernetesConfigurationClientFilterSpec extends Specification implements KubectlCommands {

    void setup() {
        KubernetesConfigurationClient.propertySourceCache.clear()
    }

    @Requires({ kubernetesApiAvailable() && KubernetesConfigurationClientFilterSpec.getConfigMaps().size() })
    void "it can filter includes config maps"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.config-maps.includes": "literal-config"], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()

        then:
        propertySources.find { it.name.startsWith 'literal-config'}

        and:
        !propertySources.find { it.name.startsWith 'game.json'}
        !propertySources.find { it.name.startsWith 'game.properties'}
        !propertySources.find { it.name.startsWith 'game.yml'}

        cleanup:
        applicationContext.close()
    }

    @Requires({ kubernetesApiAvailable() && KubernetesConfigurationClientFilterSpec.getConfigMaps().size() })
    void "it can filter excludes config maps"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.config-maps.excludes": "literal-config"], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()

        then:
        !propertySources.find { it.name.startsWith 'literal-config'}

        and:
        propertySources.find { it.name.startsWith 'game.json'}
        propertySources.find { it.name.startsWith 'game.properties'}
        propertySources.find { it.name.startsWith 'game.yml'}

        cleanup:
        applicationContext.close()
    }

    @Requires({ kubernetesApiAvailable() && KubernetesConfigurationClientFilterSpec.getConfigMaps().size() })
    void "it can have both includes and excludes filters"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.config-maps.excludes": "literal-config", "kubernetes.client.config-maps.includes": "game-config-yml"], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()

        then:
        propertySources.find { it.name.startsWith 'game.yml'}

        and:
        !propertySources.find { it.name.startsWith 'literal-config'}
        !propertySources.find { it.name.startsWith 'game.json'}
        !propertySources.find { it.name.startsWith 'game.properties'}

        cleanup:
        applicationContext.close()
    }

}
