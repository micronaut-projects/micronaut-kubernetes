package io.micronaut.kubernetes.client.operator.controller

import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.extended.workqueue.DefaultRateLimitingQueue
import io.kubernetes.client.extended.workqueue.WorkQueue
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.micronaut.kubernetes.client.operator.ControllerConfiguration
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import java.time.Duration

@MicronautTest
class DefaultControllerWatchBuilderSpec extends Specification {

    @Inject
    DefaultControllerWatchBuilder builder

    def "it creates controller watch"(){
        given:
        ControllerConfiguration operator = Stub(ControllerConfiguration.class)
        operator.getApiType() >> V1ConfigMap
        operator.getResyncCheckPeriod() >> 2000L

        WorkQueue<Request> queue = new DefaultRateLimitingQueue<>()

        when:
        def controllerWatch = builder.buildControllerWatch(operator, queue)

        then:
        controllerWatch.getResourceClass() == V1ConfigMap
        controllerWatch.getResyncPeriod() == Duration.ofMillis(2000)
    }
}
