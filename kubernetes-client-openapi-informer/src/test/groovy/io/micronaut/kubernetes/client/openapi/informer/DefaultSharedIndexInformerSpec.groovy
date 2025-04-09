package io.micronaut.kubernetes.client.openapi.informer


import io.micronaut.core.type.Argument
import io.micronaut.inject.ExecutableMethod
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject
import io.micronaut.kubernetes.client.openapi.informer.cache.Cache
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil
import spock.lang.Specification

import java.util.concurrent.ThreadFactory

class DefaultSharedIndexInformerSpec extends Specification {

    ExecutableMethod executableMethod
    ThreadFactoryUtil threadFactoryUtil

    def setup() {
        executableMethod = Stub(ExecutableMethod)
        //threadFactory = Stub(ThreadFactory)
        threadFactoryUtil = Stub(ThreadFactoryUtil)
    }

    def 'test listener resync period disabled'() {
        given:
        executableMethod.getArguments() >> new Argument[0]
        def informerApiCall = new InformerApiCall<>(executableMethod, null, executableMethod, null, "test-namespace", null)

        when:
        def informer = new DefaultSharedIndexInformer(V1Secret, "test-namespace", threadFactoryUtil, informerApiCall, 0, new Cache<>())
        informer.addEventHandler(createTestResourceEventHandler())
        informer.addEventHandlerWithResyncPeriod(createTestResourceEventHandler(), 2000)
        def listeners = informer.getProcessor().getListeners()

        then:
        listeners.size() == 2
        listeners.get(0).getResyncPeriodInMillis() == 0
        listeners.get(1).getResyncPeriodInMillis() == 0
    }

    def 'test minimal listener resync period when set on informer'() {
        given:
        executableMethod.getArguments() >> new Argument[0]
        def informerApiCall = new InformerApiCall<>(executableMethod, null, executableMethod, null, "test-namespace", null)

        when:
        def informer = new DefaultSharedIndexInformer(V1Secret, "test-namespace", threadFactoryUtil, informerApiCall, 200, new Cache<>())
        informer.addEventHandler(createTestResourceEventHandler())
        def listeners = informer.getProcessor().getListeners()

        then:
        listeners.size() == 1
        listeners.get(0).getResyncPeriodInMillis() == 1000
    }

    def 'test minimal listener resync period when set on listener'() {
        given:
        executableMethod.getArguments() >> new Argument[0]
        def informerApiCall = new InformerApiCall<>(executableMethod, null, executableMethod, null, "test-namespace", null)

        when:
        def informer = new DefaultSharedIndexInformer(V1Secret, "test-namespace", threadFactoryUtil, informerApiCall, 1000, new Cache<>())
        informer.addEventHandlerWithResyncPeriod(createTestResourceEventHandler(), 200)
        def listeners = informer.getProcessor().getListeners()

        then:
        listeners.size() == 1
        listeners.get(0).getResyncPeriodInMillis() == 1000
    }

    def 'test listener resync period lower than informer resync period'() {
        given:
        executableMethod.getArguments() >> new Argument[0]
        def informerApiCall = new InformerApiCall<>(executableMethod, null, executableMethod, null, "test-namespace", null)

        when:
        def informer = new DefaultSharedIndexInformer(V1Secret, "test-namespace", threadFactoryUtil, informerApiCall, 2000, new Cache<>())
        informer.addEventHandlerWithResyncPeriod(createTestResourceEventHandler(), 1000)
        def listeners = informer.getProcessor().getListeners()

        then:
        listeners.size() == 1
        listeners.get(0).getResyncPeriodInMillis() == 1000
    }

    ResourceEventHandler createTestResourceEventHandler() {
        return new ResourceEventHandler() {
            @Override
            void onAdd(KubernetesObject obj) {}

            @Override
            void onUpdate(KubernetesObject oldObj, KubernetesObject newObj) {}

            @Override
            void onDelete(KubernetesObject obj, boolean deletedFinalStateUnknown) {}
        }
    }
}
