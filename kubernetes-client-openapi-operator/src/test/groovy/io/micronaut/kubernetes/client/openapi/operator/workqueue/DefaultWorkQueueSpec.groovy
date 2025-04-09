package io.micronaut.kubernetes.client.openapi.operator.workqueue

import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DefaultWorkQueueSpec extends Specification {

    def 'add item' () {
        given:
        DefaultWorkQueue queue = new DefaultWorkQueue<String>()

        when: 'a new item is added'
        queue.add('item1')

        then:
        queue.length() == 1

        when: 'processing started'
        String item = queue.get()

        then:
        item == 'item1'
        queue.length() == 0

        when: 'processing completed'
        queue.done("item1")

        then:
        queue.length() == 0
    }

    def 'get item waiting' () {
        given:
        DefaultWorkQueue queue = new DefaultWorkQueue<String>()
        ExecutorService service = Executors.newCachedThreadPool()
        PollingConditions conditions = new PollingConditions(timeout: 1)

        ItemHolder itemHolder = new ItemHolder()
        service.submit(() -> itemHolder.setItem(queue.get()))

        when:
        service.submit(() -> queue.add('item1'))

        then:
        conditions.eventually {
            itemHolder.getItem() == 'item1'
        }
    }

    def 'add item after shutdown' () {
        given:
        DefaultWorkQueue queue = new DefaultWorkQueue<String>()

        when: 'a new item is added'
        queue.add('item1')

        then:
        queue.length() == 1

        when: 'shutdown initiated'
        queue.shutdown()

        then:
        queue.length() == 0

        when: 'a new item is added'
        queue.add('item2')

        then:
        queue.length() == 0
    }

    def 'add the same item while already processing it' () {
        given:
        DefaultWorkQueue queue = new DefaultWorkQueue<String>()

        when: 'a new item is added'
        queue.add('item1')

        then:
        queue.length() == 1

        when: 'processing started'
        String item = queue.get()

        then:
        item == 'item1'
        queue.length() == 0

        when: 'the same item is added'
        queue.add('item1')

        then: 'the queue length is still 0 since item1 is added to the dirty queue'
        queue.length() == 0

        when: 'processing completes'
        queue.done('item1')

        then: 'the item from the dirty queue is moved to the work queue'
        queue.length() == 1
    }

    private static class ItemHolder {
        private String item

        String getItem() {
            return item
        }

        void setItem(String item) {
            this.item = item
        }
    }
}
