package io.micronaut.kubernetes.client.informer


import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires

@MicronautTest()
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "default")
class DefaultSharedIndexInformerFactorySpec extends KubernetesSpecification {

    @Override
    def setupFixture(String namespace) {
        // no-op
    }

    def "it is created"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run()

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
        ApplicationContext applicationContext = ApplicationContext.run()

        when:
        DefaultSharedIndexInformerFactory factory = applicationContext.getBean(DefaultSharedIndexInformerFactory)

        then:
        factory.getExistingSharedIndexInformers().isEmpty()

        when:
        operations.createConfigMap("cm-test", namespace)
        def configMapList = operations.getClient(namespace).configMaps().inNamespace(namespace).list()

        def informer = factory.sharedIndexInformerFor(
                V1ConfigMap.class, V1ConfigMapList.class, "configmaps", "", namespace,
                null, null)
        sleep(1000) // the event handler is asynchronous so let's give it some time to catch up

        then:
        informer.lastSyncResourceVersion() != ""
        informer.lastSyncResourceVersion() != "0"
        informer.lastSyncResourceVersion() == configMapList.metadata.resourceVersion

        cleanup:
        operations.deleteConfigMap("cm-test", namespace)
        applicationContext.close()
    }

    def "it returns informer based on namespace and apiclass"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run()

        when:
        DefaultSharedIndexInformerFactory factory = applicationContext.getBean(DefaultSharedIndexInformerFactory)
        factory.sharedIndexInformerFor(V1ConfigMap.class, V1ConfigMapList.class, "configmaps",
                "", "default", null, null)

        then:
        factory.getExistingSharedIndexInformer("default", V1ConfigMap.class)
        factory.getExistingSharedIndexInformers()
        !factory.getExistingSharedIndexInformers().isEmpty()

        cleanup:
        applicationContext.close()
    }
}
