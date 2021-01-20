package io.micronaut.kubernetes.configuration

import com.github.stefanbirkner.systemlambda.SystemLambda
import io.fabric8.kubernetes.api.model.Pod
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Requires

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
class KubernetesConfigurationClientLabelsSpec extends KubernetesSpecification {

    void setup() {
        KubernetesConfigurationClient.propertySourceCache.clear()
    }

    void "it can filter config maps by labels"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.namespace": namespace, "kubernetes.client.config-maps.labels": [app:"game"]], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        KubernetesConfigurationClient.propertySourceCache.clear()
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
        KubernetesConfigurationClient.propertySourceCache.clear()
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
        KubernetesConfigurationClient.propertySourceCache.clear()
        propertySources.find { it.name.startsWith 'another-secret' }

        and:
        !propertySources.find { it.name.startsWith 'test-secret' }

        cleanup:
        applicationContext.close()
    }

    void "it can filter config maps by pod labels"() {
        given:
        Pod pod = TestUtils.getPods(namespace).find { it.metadata.labels && it.metadata.labels.containsKey("app.kubernetes.io/instance") }
        def envs = SystemLambda.withEnvironmentVariable("KUBERNETES_SERVICE_HOST", "localhost")
                .and("HOSTNAME", pod.metadata.name)
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.config-maps.pod-labels": ["app.kubernetes.io/instance"]], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        KubernetesConfigurationClient.propertySourceCache.clear()
        def propertySources = envs.execute(() -> Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable())

        then:
        propertySources.find { it.name.startsWith 'literal-config' }
        propertySources.find { it.name.startsWith 'game.yml' }

        and:
        !propertySources.find { it.name.startsWith 'game.json' }
        !propertySources.find { it.name.startsWith 'game.properties' }

        cleanup:
        applicationContext.close()
    }

    void "it can filter secrets by pod labels"() {
        given:
        Pod pod = TestUtils.getPods(namespace).find { it.metadata.labels && it.metadata.labels.containsKey("app.kubernetes.io/instance") }
        def envs = SystemLambda.withEnvironmentVariable("KUBERNETES_SERVICE_HOST", "localhost")
                .and("HOSTNAME", pod.metadata.name)
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.secrets.enabled": true, "kubernetes.client.secrets.pod-labels": ["app.kubernetes.io/instance"]], Environment.KUBERNETES)
        KubernetesConfigurationClient configurationClient = applicationContext.getBean(KubernetesConfigurationClient)

        when:
        KubernetesConfigurationClient.propertySourceCache.clear()
        def propertySources = envs.execute(() -> Flowable.fromPublisher(configurationClient.getPropertySources(applicationContext.environment)).blockingIterable())

        then:
        propertySources.find { it.name.startsWith 'another-secret' }

        and:
        !propertySources.find { it.name.startsWith 'test-secret' }

        cleanup:
        applicationContext.close()
    }
}
