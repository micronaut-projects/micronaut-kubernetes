package io.micronaut.kubernetes.informer

import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.informer.SharedIndexInformer
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.NoSuchBeanException
import spock.lang.Specification

class DefaultSharedIndexInformerFactorySpec extends Specification {

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
        def informer = factory.sharedIndexInformerFor(
                V1ConfigMap.class, V1ConfigMapList.class, "configmaps", "default",
                null, null)
        sleep(500) // the event handler is asynchronous so let's give it some time to catch up

        then:
        informer.lastSyncResourceVersion() != ""
        informer.lastSyncResourceVersion() != "0"

        cleanup:
        applicationContext.close()
    }

    def "it returns informer based on namespace and apiclass"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run()

        when:
        DefaultSharedIndexInformerFactory factory = applicationContext.getBean(DefaultSharedIndexInformerFactory)
        factory.sharedIndexInformerFor(V1ConfigMap.class, V1ConfigMapList.class, "configmaps",
                "default", null, null)

        then:
        factory.getExistingSharedIndexInformer("default", V1ConfigMap.class)
        factory.getExistingSharedIndexInformers()
        !factory.getExistingSharedIndexInformers().isEmpty()

        cleanup:
        applicationContext.close()
    }
}
