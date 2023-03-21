package io.micronaut.kubernetes.client.informer


import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires
import spock.util.concurrent.PollingConditions

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "informer-factory")
class DefaultSharedIndexInformerFactorySpec extends KubernetesSpecification {

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
    }

    def "it is created"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(Environment.KUBERNETES)

        expect:
        applicationContext.getBean(DefaultSharedIndexInformerFactory)

        cleanup:
        applicationContext.close()
    }

    def "it is not created when informer is disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                ["kubernetes.client.informer.enabled": "false"])

        when:
        applicationContext.getBean(DefaultSharedIndexInformerFactory)

        then:
        thrown(NoSuchBeanException)

        cleanup:
        applicationContext.close()
    }

    def "it starts informer when informer is created after the bean context was started"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(Environment.KUBERNETES)

        when:
        DefaultSharedIndexInformerFactory factory = applicationContext.getBean(DefaultSharedIndexInformerFactory)

        then:
        factory.getExistingSharedIndexInformers().isEmpty()

        when:
        def informer = factory.sharedIndexInformerFor(
                V1ConfigMap.class, V1ConfigMapList.class, "configmaps", "", namespace,
                null, null, true)

        then:
        informer.getIndexer().list().size() <= 1  // since 1.20 there's always kube-root-ca.crt, see https://github.com/kubernetes/kubernetes/blob/master/CHANGELOG/CHANGELOG-1.20.md#introducing-rootcaconfigmap

        when:
        operations.createConfigMap("cm-test", namespace)

        then:
        new PollingConditions().within(5, {
            informer.getIndexer().list().stream()
                    .filter(cm -> cm.getMetadata().getName() == "cm-test")
                    .any()
        })

        cleanup:
        operations.deleteConfigMap("cm-test", namespace)
        applicationContext.close()
    }

    def "it returns informer based on namespace and apiclass"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(Environment.KUBERNETES)

        when:
        DefaultSharedIndexInformerFactory factory = applicationContext.getBean(DefaultSharedIndexInformerFactory)
        factory.sharedIndexInformerFor(
                V1ConfigMap.class,
                V1ConfigMapList.class,
                "configmaps",
                "",
                "default",
                null,
                null,
                true)

        then:
        factory.getExistingSharedIndexInformer("default", V1ConfigMap.class)
        factory.getExistingSharedIndexInformers()
        !factory.getExistingSharedIndexInformers().isEmpty()

        cleanup:
        applicationContext.close()
    }
}
