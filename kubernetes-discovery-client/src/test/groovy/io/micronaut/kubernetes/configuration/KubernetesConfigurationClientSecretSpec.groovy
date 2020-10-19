package io.micronaut.kubernetes.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Requires

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesConfigurationClientSecretSpec extends KubernetesSpecification {

    void setup() {
        KubernetesConfigurationClient.propertySourceCache.clear()
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
        def propertySource = Flowable.fromPublisher(
                configurationClient.getPropertySources(applicationContext.environment)).
                blockingIterable().find { it.name.startsWith 'test-secret' } as PropertySource

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
        def propertySources = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()

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
        def propertySources = Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable()

        then:
        propertySources.find { it.name.startsWith 'test-secret'}

        and:
        !propertySources.find { it.name.startsWith 'another-secret'}

        cleanup:
        applicationContext.close()
    }
}
