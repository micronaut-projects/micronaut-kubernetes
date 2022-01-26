package io.micronaut.kubernetes.client.operator.controller


import io.kubernetes.client.extended.controller.DefaultController
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.context.annotation.Property
import io.micronaut.kubernetes.client.operator.ResourceReconciler
import io.micronaut.kubernetes.client.operator.ControllerConfiguration
import io.micronaut.kubernetes.client.operator.configuration.OperatorConfigurationProperties
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = "kubernetes.client.api-discovery.enabled", value="false")
class DefaultControllerBuilderSpec extends Specification {

    @Inject
    ControllerBuilder controllerBuilder

    def "it builds controller"() {
        given:
        ControllerConfiguration operator = Stub(ControllerConfiguration.class)
        operator.getName() >> "controller-V1ConfigMap"
        operator.getApiType() >> V1ConfigMap
        operator.getApiListType() >> V1ConfigMapList
        operator.getResourcePlural() >> 'configmaps'
        operator.getApiGroup() >> ''
        operator.getNamespaces() >> ['default']
        operator.getLabelSelector() >> ""

        ResourceReconciler<V1ConfigMap> reconciler = Stub(ResourceReconciler<V1ConfigMap>.class)

        when:
        DefaultController controller = controllerBuilder.build(operator, reconciler)

        then:
        controller.name == "controller-V1ConfigMap"
        controller.workerCount == OperatorConfigurationProperties.DEFAULT_WORKER_COUNT.toInteger()
        controller.workQueue
    }
}
