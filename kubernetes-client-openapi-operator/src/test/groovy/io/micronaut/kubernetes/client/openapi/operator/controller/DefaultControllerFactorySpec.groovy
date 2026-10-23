package io.micronaut.kubernetes.client.openapi.operator.controller

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import io.micronaut.kubernetes.client.openapi.operator.configuration.OperatorConfiguration
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler
import io.micronaut.kubernetes.client.openapi.operator.workqueue.RateLimitingQueue
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DefaultControllerFactorySpec extends Specification {

    def 'pending startup is discarded after leadership loss even when leadership is reacquired'() {
        given:
        def syncCheckEntered = new CountDownLatch(1)
        def releaseSyncCheck = new CountDownLatch(1)
        def newSessionStarted = new CountDownLatch(1)
        def syncChecks = new AtomicInteger()
        def resyncs = new AtomicInteger()
        def starts = new AtomicInteger()
        def informer = Stub(SharedIndexInformer) {
            hasSynced() >> {
                if (syncChecks.getAndIncrement() == 0) {
                    syncCheckEntered.countDown()
                    assert releaseSyncCheck.await(5, TimeUnit.SECONDS)
                }
                true
            }
            resyncListeners() >> {
                resyncs.incrementAndGet()
                newSessionStarted.countDown()
            }
        }
        def informerFactory = Stub(SharedIndexInformerFactory) {
            getExistingSharedIndexInformer(V1Pod, null) >> informer
        }
        def configuration = Stub(OperatorConfiguration) {
            getWorkerCount() >> 1
            getReadyTimeout() >> Duration.ofSeconds(5)
            getReadyCheckInternal() >> Duration.ofMillis(10)
        }
        def queue = Stub(RateLimitingQueue) {
            start() >> { starts.incrementAndGet() }
            isShutdown() >> true
        }
        def registry = new SimpleMeterRegistry()
        def factory = new DefaultControllerFactory(informerFactory,
            new ThreadFactoryUtil(Executors.defaultThreadFactory()), configuration, registry)
        def controller = factory.createController('test', V1Pod, null, Stub(ResourceReconciler), queue)

        when:
        factory.startControllers()
        assert syncCheckEntered.await(5, TimeUnit.SECONDS)
        factory.stopControllers()
        if (reacquire) {
            factory.startControllers()
            assert newSessionStarted.await(5, TimeUnit.SECONDS)
        }
        releaseSyncCheck.countDown()
        // Drain startup tasks without cancelling them, so late activation cannot escape the assertions.
        factory.@executorService.shutdown()
        assert factory.@executorService.awaitTermination(5, TimeUnit.SECONDS)

        then:
        starts.get() == expectedStarts
        resyncs.get() == expectedStarts

        cleanup:
        releaseSyncCheck.countDown()
        factory.destroy()
        controller.@workerThreadPool.shutdownNow()
        registry.close()

        where:
        reacquire | expectedStarts
        false     | 0
        true      | 1
    }
}
