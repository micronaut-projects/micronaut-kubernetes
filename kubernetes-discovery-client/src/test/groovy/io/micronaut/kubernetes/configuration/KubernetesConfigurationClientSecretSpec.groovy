package io.micronaut.kubernetes.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.TestUtils
import io.reactivex.Flowable
import spock.lang.Requires
import spock.lang.Specification

class KubernetesConfigurationClientSecretSpec extends Specification implements KubectlCommands {

    void setup() {
        KubernetesConfigurationClient.emptyPropertySourceCache()
    }

    @Requires({ TestUtils.secretExists('test-secret')})
    void "it can read secrets"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.secrets.enabled": true], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySource = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable().find { it.name.startsWith 'test-secret' } as PropertySource

        then:
        propertySource.name == 'test-secret (Kubernetes Secret)'
        propertySource.get('username') == 'my-app'
        propertySource.get('password') == '39528$vdg7Jb'
    }
}
