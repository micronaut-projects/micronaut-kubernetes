package io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter

import spock.lang.Specification

import java.time.Duration

class MaxOfRateLimiterSpec extends Specification {

    def 'test delay repeated'() {
        given:
        def rateLimiter = new MaxOfRateLimiter<>(
                Arrays.asList(
                        new ItemFastSlowRateLimiter(Duration.ofSeconds(2), Duration.ofSeconds(3), 3),
                        new ItemExponentialFailureRateLimiter(Duration.ofSeconds(1), Duration.ofSeconds(5))))

        when:
        Duration delay = rateLimiter.when('item1')

        then:
        rateLimiter.numRequeues('item1') == 1
        delay == Duration.ofSeconds(2)

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
        delay == Duration.ofSeconds(5)

        when: "reset method is called"
        rateLimiter.reset()
        delay = rateLimiter.when('item1')

        then: "number of requeues should be reset"
        rateLimiter.numRequeues('item1') == 1
        delay == Duration.ofSeconds(2)
    }
}
