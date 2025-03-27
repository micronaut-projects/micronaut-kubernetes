package io.micronaut.kubernetes.client.openapi.operator.workqueue

import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.util.concurrent.Executors

class DefaultDelayingQueueSpec extends Specification {

    def 'add multiple items'() {
        given:
        DefaultDelayingQueue<String> queue = new DefaultDelayingQueue<>(Executors.newSingleThreadExecutor())

        when:
        queue.addAfter("item0", Duration.ZERO)
        queue.addAfter("item1", Duration.ofMillis(100))
        queue.addAfter("item2", Duration.ofMillis(300))
        queue.addAfter("item3", Duration.ofMillis(200))
        queue.addAfter("item4", Duration.ofMillis(150))
        PollingConditions conditions = new PollingConditions(timeout: 2)

        then:
        queue.length() == 1
        queue.get() == "item0"
        conditions.eventually {
            queue.length() == 4
        }
        queue.get() == "item1"
        queue.get() == "item4"
        queue.get() == "item3"
        queue.get() == "item2"

        cleanup:
        queue.shutdown()
    }

    def 'add already added item'() {
        given:
        DefaultDelayingQueue<String> queue = new DefaultDelayingQueue<>(Executors.newSingleThreadExecutor())

        when:
        queue.addAfter("item1", Duration.ofMillis(300))
        queue.addAfter("item2", Duration.ofMillis(500))
        queue.addAfter("item3", Duration.ofMillis(400))
        queue.addAfter("item2", Duration.ofMillis(250))
        PollingConditions conditions = new PollingConditions(timeout: 2)

        then:
        queue.length() == 0
        conditions.eventually {
            queue.length() == 3
        }
        queue.get() == "item2"
        queue.get() == "item1"
        queue.get() == "item3"

        cleanup:
        queue.shutdown()
    }
}
