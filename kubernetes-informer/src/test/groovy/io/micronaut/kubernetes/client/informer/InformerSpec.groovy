package io.micronaut.kubernetes.client.informer

import io.kubernetes.client.informer.SharedInformerFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Specification

class InformerSpec extends Specification {

    def "factory is created"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(Environment.KUBERNETES)

        expect:
        applicationContext.getBean(SharedInformerFactory)
    }

}
