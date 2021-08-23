package io.micronaut.kubernetes.informer

import io.kubernetes.client.informer.SharedInformerFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.NoSuchBeanException
import spock.lang.Specification

class DefaultSharedInformerFactorySpec extends Specification {

    def "factory is created"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run()

        expect:
        applicationContext.getBean(SharedInformerFactory)
    }

    def "factory is not created when disabled"(){
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.informer.enabled": "false"])

        when:
        applicationContext.getBean(SharedInformerFactory)

        then:
        thrown(NoSuchBeanException)
    }
}
