package io.micronaut.kubernetes.client.openapi.informer


import io.micronaut.kubernetes.client.openapi.informer.cache.Cache
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import spock.lang.Specification

import java.util.concurrent.Executors

class DeltaFifoSpec extends Specification {

    def 'relist removes an unconsumed object using its latest queued state'() {
        given:
        def oldPod = new V1Pod().metadata(new V1ObjectMeta().name('pod').namespace('default').resourceVersion('1'))
        def newPod = new V1Pod().metadata(new V1ObjectMeta().name('pod').namespace('default').resourceVersion('2'))
        def cache = new Cache<V1Pod>()
        if (cached) {
            cache.add(oldPod)
        }
        def fifo = new DeltaFifo(cache)
        def consumer = new DeltaConsumer<V1Pod>(fifo, cache,
            new SharedProcessor<V1Pod>(Executors.defaultThreadFactory()), null)
        def result = []
        fifo.add(DeltaFifo.DeltaType.ADDED, oldPod)
        fifo.add(DeltaFifo.DeltaType.UPDATED, newPod)

        when:
        fifo.replace([])
        fifo.replace([])
        fifo.pop { deltas ->
            result.addAll(deltas)
            consumer.handleDeltas(deltas)
        }

        then:
        result*.type() == [DeltaFifo.DeltaType.ADDED, DeltaFifo.DeltaType.UPDATED, DeltaFifo.DeltaType.DELETED]
        result.last().object() instanceof DeletedFinalStateUnknown
        result.last().object().key() == 'default/pod'
        result.last().object().object().is(newPod)
        cache.list().empty

        where:
        cached << [false, true]
    }

    def 'relist preserves a queued explicit deletion'() {
        given:
        def pod = new V1Pod().metadata(new V1ObjectMeta().name('pod'))
        def fifo = new DeltaFifo(new Cache<>())
        def result = []
        fifo.add(DeltaFifo.DeltaType.ADDED, pod)
        fifo.add(DeltaFifo.DeltaType.DELETED, pod)

        when:
        fifo.replace([])
        fifo.pop { result.addAll(it) }

        then:
        result*.type() == [DeltaFifo.DeltaType.ADDED, DeltaFifo.DeltaType.DELETED]
        result.last().object().is(pod)
    }

    def 'relist retains queued objects still present in the new list'() {
        given:
        def pod = new V1Pod().metadata(new V1ObjectMeta().name('pod').resourceVersion('1'))
        def fifo = new DeltaFifo(new Cache<>())
        def result = []
        fifo.add(DeltaFifo.DeltaType.ADDED, pod)

        when:
        fifo.replace([pod])
        fifo.pop { result.addAll(it) }

        then:
        result*.type() == [DeltaFifo.DeltaType.SYNC]
        result.last().object().is(pod)
    }

    def 'initial relist waits for cached deletions as well as listed objects to be consumed'() {
        given:
        def deletedPod = new V1Pod().metadata(new V1ObjectMeta().name('deleted'))
        def listedPod = new V1Pod().metadata(new V1ObjectMeta().name('listed'))
        def cache = new Cache<V1Pod>()
        cache.add(deletedPod)
        def fifo = new DeltaFifo(cache)
        def result = []

        when:
        fifo.replace([listedPod])
        fifo.pop { result.addAll(it) }

        then:
        !fifo.hasSynced()
        result*.type() == [DeltaFifo.DeltaType.SYNC]

        when:
        fifo.pop { result.addAll(it) }

        then:
        fifo.hasSynced()
        result.last().type() == DeltaFifo.DeltaType.DELETED
        result.last().object().object().is(deletedPod)
    }

    def 'test pod added delta'() {
        given:
        def pod = new V1Pod().metadata(new V1ObjectMeta().name("foo1").namespace("default"))
        def deltaFifo = new DeltaFifo(new Cache<>())
        def deltasResult = []

        when:
        deltaFifo.add(DeltaFifo.DeltaType.ADDED, pod)
        deltaFifo.pop(deltas -> deltasResult.add(deltas.peekFirst()))

        then:
        deltasResult.size() == 1
        deltasResult.get(0).type() == DeltaFifo.DeltaType.ADDED
        deltasResult.get(0).object() == pod
    }

    def 'test pod updated delta'() {
        given:
        def pod = new V1Pod().metadata(new V1ObjectMeta().name("foo1").namespace("default"))
        def deltaFifo = new DeltaFifo(new Cache<>())
        def deltasResult = []

        when:
        deltaFifo.add(DeltaFifo.DeltaType.UPDATED, pod)
        deltaFifo.pop(deltas -> deltasResult.add(deltas.peekFirst()))

        then:
        deltasResult.size() == 1
        deltasResult.get(0).type() == DeltaFifo.DeltaType.UPDATED
        deltasResult.get(0).object() == pod
    }

    def 'test pod deleted delta'() {
        given:
        def pod = new V1Pod().metadata(new V1ObjectMeta().name("foo1").namespace("default"))
        def cache = new Cache<>()
        def deltaFifo = new DeltaFifo(cache)
        def deltasResult = []

        when:
        cache.add(pod)
        deltaFifo.add(DeltaFifo.DeltaType.DELETED, pod)
        deltaFifo.pop(deltas -> deltasResult.add(deltas.peekFirst()))

        then:
        deltasResult.size() == 1
        deltasResult.get(0).type() == DeltaFifo.DeltaType.DELETED
        deltasResult.get(0).object() == pod
    }

    def 'test pod replaced delta'() {
        given:
        def pod = new V1Pod().metadata(new V1ObjectMeta().name("foo1").namespace("default"))
        def deltaFifo = new DeltaFifo(new Cache<>())
        def deltasResult = []

        when:
        deltaFifo.replace([pod])
        deltaFifo.pop(deltas -> deltasResult.add(deltas.peekFirst()))

        then:
        deltasResult.size() == 1
        deltasResult.get(0).type() == DeltaFifo.DeltaType.SYNC
        deltasResult.get(0).object() == pod
    }

    def 'test pod deleted delta dedup'() {
        given:
        def pod = new V1Pod().metadata(new V1ObjectMeta().name("foo1").namespace("default"))
        def cache = new Cache<>()
        def deltaFifo = new DeltaFifo(cache)
        def deltasResult = []

        when:
        cache.add(pod)
        deltaFifo.add(DeltaFifo.DeltaType.ADDED, pod)
        deltaFifo.add(DeltaFifo.DeltaType.DELETED, pod)
        deltaFifo.pop(deltas -> {
            def delta = deltas.poll()
            while (delta != null) {
                deltasResult.add(delta)
                delta = deltas.poll()
            }
        })

        then:
        deltasResult.size() == 2
        deltasResult.get(0).type() == DeltaFifo.DeltaType.ADDED
        deltasResult.get(0).object() == pod
        deltasResult.get(1).type() == DeltaFifo.DeltaType.DELETED
        deltasResult.get(1).object() == pod

        when:
        deltasResult.clear()
        cache.add(pod)
        deltaFifo.add(DeltaFifo.DeltaType.ADDED, pod)
        deltaFifo.add(DeltaFifo.DeltaType.DELETED, pod)
        deltaFifo.add(DeltaFifo.DeltaType.DELETED, pod)
        deltaFifo.pop(deltas -> {
            def delta = deltas.poll()
            while (delta != null) {
                deltasResult.add(delta)
                delta = deltas.poll()
            }
        })

        then:
        deltasResult.size() == 2
        deltasResult.get(0).type() == DeltaFifo.DeltaType.ADDED
        deltasResult.get(0).object() == pod
        deltasResult.get(1).type() == DeltaFifo.DeltaType.DELETED
        deltasResult.get(1).object() == pod
    }

    def 'test resync'() {
        given:
        def pod = new V1Pod().metadata(new V1ObjectMeta().name("foo1").namespace("default"))
        def cache = new Cache<>()
        def deltaFifo = new DeltaFifo(cache)
        def deltasResult = []

        when:
        cache.add(pod)
        deltaFifo.resync()
        deltaFifo.pop(deltas -> deltasResult.add(deltas.peekFirst()))

        then:
        deltasResult.size() == 1
        deltasResult.get(0).type() == DeltaFifo.DeltaType.SYNC
        deltasResult.get(0).object() == pod
    }
}
