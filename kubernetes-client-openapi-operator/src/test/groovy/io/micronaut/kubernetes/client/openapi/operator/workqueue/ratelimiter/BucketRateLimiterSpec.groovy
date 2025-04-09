package io.micronaut.kubernetes.client.openapi.operator.workqueue.ratelimiter

import spock.lang.Specification

import java.time.Duration

class BucketRateLimiterSpec extends Specification {

    def 'test delay repeated'() {
        given:
        def rateLimiter = new BucketRateLimiter(2, 2, Duration.ofSeconds(1))

        when:
        Duration delay = rateLimiter.when('item1')

        then:
        delay == Duration.ZERO

        when:
        delay = rateLimiter.when('item1')

        then:
        delay == Duration.ZERO

        when:
        delay = rateLimiter.when('item1')

        then:
        delay.toMillis() > 0

        when:
        Thread.sleep(1000)
        delay = rateLimiter.when('item1')

        then:
        delay == Duration.ZERO
    }
}
