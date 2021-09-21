package io.micronaut.kubernetes.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.NoSuchBeanException
import spock.lang.Specification

class DiscoveryFactorySpec extends Specification {

    def "it resolves factory"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run()

        expect:
        applicationContext.getBean(DiscoveryFactory)

        cleanup:
        applicationContext.close()
    }

    def "it fails to resolve factory when it is disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.api-discovery.enabled": false])

        when:
        applicationContext.getBean(DiscoveryFactory)

        then:
        thrown(NoSuchBeanException)

        cleanup:
        applicationContext.close()
    }
}
