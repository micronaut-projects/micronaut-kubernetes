package io.micronaut.kubernetes.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Requires
import spock.lang.Specification

import javax.inject.Inject

import static io.micronaut.kubernetes.test.TestUtils.configMapExists

@MicronautTest(environments = [Environment.KUBERNETES])
class KubernetesConfigurationClientSpec extends Specification implements KubectlCommands {

    @Inject
    KubernetesConfigurationClient configurationClient

    @Inject
    ApplicationContext applicationContext

    @Requires({ configMapExists('game-config-properties')})
    void "it can read config maps from properties"() {
        when:
        def propertySource = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable().find { it.name == 'game.properties' }

        then:
        propertySource.name == 'game.properties'
        propertySource.get('enemies') == 'zombies'
        propertySource.get('lives') == '5'
        propertySource.get('enemies.cheat.level') == 'noGoodRotten'
    }

    @Requires({ configMapExists('game-config-yml')})
    void "it can read config maps from yml"() {
        when:
        def propertySource = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable().find { it.name == 'game.yml' }

        then:
        propertySource.name == 'game.yml'
        propertySource.get('enemies') == 'aliens'
        propertySource.get('lives') == 3
        propertySource.get('enemies.cheat.level') == 'noGoodRotten'
    }

    @Requires({ configMapExists('literal-config')})
    void "it can read config maps from literals"() {
        given:
        def itr = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()
        println itr*.properties

        when:
        def propertySource = itr.find { it.name == 'literal-config' }

        then:
        propertySource.name == 'literal-config'
        propertySource.get('special.how') == 'very'
        propertySource.get('special.type') == 'charm'
    }

}
