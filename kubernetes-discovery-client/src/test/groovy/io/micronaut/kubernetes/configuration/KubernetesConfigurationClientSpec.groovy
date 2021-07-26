package io.micronaut.kubernetes.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.env.EnvironmentPropertySource
import io.micronaut.context.env.PropertySource
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Requires
import spock.lang.Shared

import jakarta.inject.Inject

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesConfigurationClientSpec extends KubernetesSpecification {

    @Inject
    @Shared
    KubernetesConfigurationClient configurationClient

    @Inject
    @Shared
    ApplicationContext applicationContext

    void setup() {
        KubernetesConfigurationClient.propertySourceCache.clear()
        applicationContext.environment.refresh()
    }

    void cleanup() {
        setup()
    }

    void "it can read config maps from properties"() {
        when:
        PropertySource propertySource = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable().find { it.name.startsWith 'game.properties' }

        then:
        propertySource.name == 'game.properties (Kubernetes ConfigMap)'
        propertySource.order > EnvironmentPropertySource.POSITION
        propertySource.get('enemies') == 'zombies'
        propertySource.get('lives') == '5'
        propertySource.get('enemies.cheat.level') == 'noGoodRotten'
        propertySource.get(KubernetesConfigurationClient.CONFIG_MAP_RESOURCE_VERSION)
    }

    void "it can read config maps from yml"() {
        when:
        PropertySource propertySource = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable().find { it.name.startsWith 'game.yml' }

        then:
        propertySource.name == 'game.yml (Kubernetes ConfigMap)'
        propertySource.order > EnvironmentPropertySource.POSITION
        propertySource.get('enemies') == 'aliens'
        propertySource.get('lives') == 3
        propertySource.get('enemies.cheat.level') == 'noGoodRotten'
        propertySource.get(KubernetesConfigurationClient.CONFIG_MAP_RESOURCE_VERSION)
    }

    void "it can read config maps from json"() {
        when:
        PropertySource propertySource = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable().find { it.name.startsWith 'game.json' }

        then:
        propertySource.name == 'game.json (Kubernetes ConfigMap)'
        propertySource.order > EnvironmentPropertySource.POSITION
        propertySource.get('enemies') == 'monsters'
        propertySource.get('lives') == 7
        propertySource.get(KubernetesConfigurationClient.CONFIG_MAP_RESOURCE_VERSION)
    }

    void "it can read config maps from literals"() {
        given:
        KubernetesConfigurationClient.propertySourceCache.clear()

        when:
        PropertySource propertySource = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable().find { it.name.startsWith 'literal-config' }

        then:
        propertySource.name == 'literal-config (Kubernetes ConfigMap)'
        propertySource.order > EnvironmentPropertySource.POSITION
        propertySource.get('special.how') == 'very'
        propertySource.get('special.type') == 'charm'
    }

    void "secret access is disabled by default"() {
        when:
        def propertySource = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable().find { it.name.startsWith 'test-secret' }

        then:
        !propertySource
    }

    void "it can read empty config maps"(){
        given:
        KubernetesConfigurationClient.propertySourceCache.clear()
        operations.createConfigMap("empty-map", namespace, [:])

        when:
        Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable().find { it.name.startsWith 'empty-map' }

        then:
        noExceptionThrown()

        cleanup:
        operations.deleteConfigMap("empty-map", namespace)
    }
}
