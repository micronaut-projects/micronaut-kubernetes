package io.micronaut.kubernetes.client.informer

import io.kubernetes.client.openapi.ApiClient
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared

@MicronautTest(environments = [Environment.KUBERNETES])
@spock.lang.Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "micronaut-api-client-creation")
@Property(name = "spec.reuseNamespace", value = "false")
@Property(name = "spec.name", value = "ApiClientCreationListenerSpec")
class ApiClientCreationListenerSpec  extends KubernetesSpecification {

    @Shared
    @Inject
    ApplicationContext applicationContext

    static boolean eventFired = false

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)
    }

    def "should fire"() {
        expect:
        !eventFired

        when:
        applicationContext.getBean(ApiClient)

        then:
        eventFired
    }

    @Requires(property = "spec.name", value = "ApiClientCreationListenerSpec")
    @Singleton
    static class K8sClientEventListener implements BeanCreatedEventListener<ApiClient> {

        @Override
        public ApiClient onCreated(BeanCreatedEvent<ApiClient> event) {
            eventFired = true
            return event.getBean();
        }
    }
}
