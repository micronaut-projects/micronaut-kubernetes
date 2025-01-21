package io.micronaut.kubernetes.client.openapi.informer

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = "kubernetes.client.informer.enabled", value = "false")
class ApiExecMethodProcessorSpec extends Specification {

    @Inject
    ApiReactorExecMethodProcessor apiReactorExecMethodProcessor

    @Inject
    ApiWatcherExecMethodProcessor apiWatcherExecMethodProcessor

    def 'test exec method processor maps'() {
        when:
        def reactorBeanTypes = apiReactorExecMethodProcessor.getBeanTypes()
        def reactorGlobalExecMethods = apiReactorExecMethodProcessor.getGlobalExecMethods()
        def reactorNamespaceExecMethods = apiReactorExecMethodProcessor.getNamespaceExecMethods()
        def watcherBeanTypes = apiWatcherExecMethodProcessor.getBeanTypes()
        def watcherGlobalExecMethods = apiWatcherExecMethodProcessor.getGlobalExecMethods()
        def watcherNamespaceExecMethods = apiWatcherExecMethodProcessor.getNamespaceExecMethods()

        then:
        reactorBeanTypes.size() != 0
        reactorGlobalExecMethods.size() != 0
        reactorNamespaceExecMethods.size() != 0
        reactorBeanTypes.keySet() == watcherBeanTypes.keySet()
        reactorGlobalExecMethods.keySet() == watcherGlobalExecMethods.keySet()
        reactorNamespaceExecMethods.keySet() == watcherNamespaceExecMethods.keySet()
    }
}
