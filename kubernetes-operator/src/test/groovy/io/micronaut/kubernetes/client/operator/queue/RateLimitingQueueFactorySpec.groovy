package io.micronaut.kubernetes.client.operator.queue

import io.kubernetes.client.extended.workqueue.RateLimitingQueue
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(environments = [Environment.KUBERNETES])
class RateLimitingQueueFactorySpec extends Specification {

    @Inject
    ApplicationContext applicationContext

    def "it creates queue for every name"() {
        when:
        def q1 = applicationContext.createBean(RateLimitingQueue)
        applicationContext.registerSingleton(RateLimitingQueue.class, q1, Qualifiers.byName("q1"))

        then:
        noExceptionThrown()

        when:
        def q2 = applicationContext.getBean(RateLimitingQueue, Qualifiers.byName("q1"))

        then:
        q1 == q2

        when:
        def q3 = applicationContext.createBean(RateLimitingQueue)
        applicationContext.registerSingleton(RateLimitingQueue.class, q1, Qualifiers.byName("q2"))

        then:
        q1 == q2
        q1 != q3
    }
}
