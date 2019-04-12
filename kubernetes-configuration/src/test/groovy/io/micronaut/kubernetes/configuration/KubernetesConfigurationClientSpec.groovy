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

    @Requires({ configMapExists('game-config')})
    void "it can read config maps"() {
        when:
        def propertySource = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingFirst()

        then:
        propertySource.name == 'game.properties'
        propertySource.get('enemies') == 'aliens'
        propertySource.get('lives') == '3'
    }

}
