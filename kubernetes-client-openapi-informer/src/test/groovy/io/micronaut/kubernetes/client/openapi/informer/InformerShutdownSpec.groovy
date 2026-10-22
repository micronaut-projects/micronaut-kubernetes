package io.micronaut.kubernetes.client.openapi.informer

import io.micronaut.core.type.Argument
import io.micronaut.inject.ExecutableMethod
import io.micronaut.kubernetes.client.openapi.informer.cache.Cache
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

class InformerShutdownSpec extends Specification {

    def 'worker shutdown terminates an idle worker and cannot be undone by a late run'() {
        given:
        def cache = new Cache<V1Pod>()
        def worker = listener
            ? new ProcessorListener<V1Pod>(Stub(ResourceEventHandler), 0)
            : new DeltaConsumer<V1Pod>(new DeltaFifo(cache), cache,
                new SharedProcessor<V1Pod>(Executors.defaultThreadFactory()), null)
        def thread = new Thread(worker)

        when:
        if (stopBeforeRun) {
            worker.stop()
        }
        thread.start()
        if (!stopBeforeRun) {
            new PollingConditions(timeout: 5).eventually {
                assert thread.state == Thread.State.WAITING
            }
            worker.stop()
            worker.stop()
        }
        thread.join(5000)

        then:
        !thread.alive

        cleanup:
        thread.interrupt()
        thread.join(5000)

        where:
        listener | stopBeforeRun
        true     | false
        false    | false
        true     | true
        false    | true
    }

    def 'informer shutdown terminates its listener and delta consumer threads'() {
        given:
        def threads = new CopyOnWriteArrayList<Thread>()
        def threadFactory = { Runnable runnable ->
            def thread = new Thread(runnable)
            threads.add(thread)
            thread
        } as ThreadFactory
        def method = Stub(ExecutableMethod) {
            getArguments() >> new Argument[0]
            invoke(_, _) >> Mono.never()
        }
        def apiCall = new InformerApiCall(method, new Object(), method, new Object(), null, null)
        def informer = new DefaultSharedIndexInformer(V1Pod, null,
            new ThreadFactoryUtil(threadFactory), apiCall, 0, new Cache<V1Pod>())
        informer.addEventHandler(Stub(ResourceEventHandler))

        when:
        if (stopBeforeRun) {
            informer.stop()
        }
        informer.run()
        if (!stopBeforeRun) {
            new PollingConditions(timeout: 5).eventually {
                assert threads.size() == 2
                assert threads.every { it.state == Thread.State.WAITING }
            }
        }
        informer.stop()
        informer.stop()
        threads.each { it.join(5000) }

        then:
        threads.size() == expectedThreads
        threads.every { !it.alive }

        cleanup:
        informer.stop()
        threads.each { it.interrupt() }
        threads.each { it.join(5000) }

        where:
        stopBeforeRun | expectedThreads
        false         | 2
        true          | 0
    }
}
