package io.micronaut.kubernetes.configuration

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.kubernetes.test.KubectlCommands
import io.micronaut.kubernetes.test.TestUtils
import io.reactivex.Flowable
import spock.lang.Requires
import spock.lang.Specification

import static io.micronaut.kubernetes.test.TestUtils.kubernetesApiAvailable

@Slf4j
class KubernetesConfigurationClientSecretSpec extends Specification implements KubectlCommands {

    void setup() {
        KubernetesConfigurationClient.propertySourceCache.clear()
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

        cleanup:
        applicationContext.close()
    }

    @Requires({ kubernetesApiAvailable() && KubernetesConfigurationClientFilterSpec.getSecrets().size() })
    void "it can filter includes secrets"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.secrets.enabled": true, "kubernetes.client.secrets.includes": "another-secret"], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()

        then:
        propertySources.find { it.name.startsWith 'another-secret'}

        and:
        !propertySources.find { it.name.startsWith 'test-secret'}

        cleanup:
        applicationContext.close()
    }

    @Requires({ kubernetesApiAvailable() && KubernetesConfigurationClientFilterSpec.getSecrets().size() })
    void "it can filter excludes secrets"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.secrets.enabled": true, "kubernetes.client.secrets.excludes": "another-secret"], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        def propertySources = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()

        then:
        propertySources.find { it.name.startsWith 'test-secret'}

        and:
        !propertySources.find { it.name.startsWith 'another-secret'}

        cleanup:
        applicationContext.close()
    }
}
