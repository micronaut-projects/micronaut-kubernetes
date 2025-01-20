package io.micronaut.kubernetes.client.openapi.informer


import io.micronaut.kubernetes.client.openapi.informer.cache.Cache
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Pod
import spock.lang.Specification

class DeltaFifoSpec extends Specification {

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
        deltasResult.get(0).getKey() == DeltaFifo.DeltaType.ADDED
        deltasResult.get(0).getValue() == pod
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
        deltasResult.get(0).getKey() == DeltaFifo.DeltaType.UPDATED
        deltasResult.get(0).getValue() == pod
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
        deltasResult.get(0).getKey() == DeltaFifo.DeltaType.DELETED
        deltasResult.get(0).getValue() == pod
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
        deltasResult.get(0).getKey() == DeltaFifo.DeltaType.SYNC
        deltasResult.get(0).getValue() == pod
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
        deltasResult.get(0).getKey() == DeltaFifo.DeltaType.ADDED
        deltasResult.get(0).getValue() == pod
        deltasResult.get(1).getKey() == DeltaFifo.DeltaType.DELETED
        deltasResult.get(1).getValue() == pod

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
        deltasResult.get(0).getKey() == DeltaFifo.DeltaType.ADDED
        deltasResult.get(0).getValue() == pod
        deltasResult.get(1).getKey() == DeltaFifo.DeltaType.DELETED
        deltasResult.get(1).getValue() == pod
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
        deltasResult.get(0).getKey() == DeltaFifo.DeltaType.SYNC
        deltasResult.get(0).getValue() == pod
    }
}
