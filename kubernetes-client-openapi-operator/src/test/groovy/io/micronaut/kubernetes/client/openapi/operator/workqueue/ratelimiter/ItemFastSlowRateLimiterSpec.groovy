package io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter


import spock.lang.Specification

import java.time.Duration

class ItemFastSlowRateLimiterSpec extends Specification {

    def 'test delay repeated'() {
        given:
        Duration fastDelay = Duration.ofSeconds(1)
        Duration slowDelay = Duration.ofSeconds(3)
        def rateLimiter = new ItemFastSlowRateLimiter(fastDelay, slowDelay, 2)

        when:
        Duration delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 1
        delay == fastDelay

        when:
        delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 2
        delay == fastDelay

        when:
        delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 3
        delay == slowDelay

        when: "reset method is called"
        rateLimiter.reset()
        delay = rateLimiter.when('item1')

        then: "number of requeues should be reset"
        rateLimiter.numRequeues('item1') == 1
        delay == fastDelay

        when:
        delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 2
        delay == fastDelay

        when:
        delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 3
        delay == slowDelay

        when: "forget method is called"
        rateLimiter.forget('item1')
        delay = rateLimiter.when('item1')

        then: "number of requeues should be reset"
        rateLimiter.numRequeues('item1') == 1
        delay == fastDelay
    }
}
