package io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter

import spock.lang.Specification

import java.time.Duration

class ItemExponentialFailureRateLimiterSpec extends Specification {

    def 'test delay repeated'() {
        given:
        Duration baseDelay = Duration.ofSeconds(1)
        Duration maxDelay = Duration.ofSeconds(5)
        def rateLimiter = new ItemExponentialFailureRateLimiter(baseDelay, maxDelay)

        when:
        Duration delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 1
        delay == Duration.ofSeconds(1)

        when:
        delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 2
        delay == Duration.ofSeconds(2)

        when:
        delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 3
        delay == Duration.ofSeconds(4)

        when:
        delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 4
        delay == maxDelay

        when: "reset method is called"
        rateLimiter.reset()
        delay = rateLimiter.when('item1')

        then: "number of requeues should be reset"
        rateLimiter.numRequeues('item1') == 1
        delay == Duration.ofSeconds(1)

        when:
        delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 2
        delay == Duration.ofSeconds(2)

        when:
        delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 3
        delay == Duration.ofSeconds(4)

        when:
        delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 4
        delay == maxDelay

        when: "forget method is called"
        rateLimiter.forget('item1')
        delay = rateLimiter.when('item1')

        then: "number of requeues should be reset"
        rateLimiter.numRequeues('item1') == 1
        delay == Duration.ofSeconds(1)
    }
}
