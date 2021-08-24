package io.micronaut.kubernetes.informer

import io.kubernetes.client.informer.SharedInformerFactory
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class InformerSpec extends Specification {

    def "factory is created"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run()

        expect:
        applicationContext.getBean(SharedInformerFactory)
    }

}
