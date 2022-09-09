package io.micronaut.kubernetes.client.operator.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Specification

import java.time.Duration

class ControllerConfigurationPropertiesSpec extends Specification {

    def "it resolves operator configuration defaults"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run(Environment.KUBERNETES)

        then:
        applicationContext.getBean(OperatorConfigurationProperties)

        when:
        def config = applicationContext.getBean(OperatorConfigurationProperties)

        then:
        config.getWorkerCount() == OperatorConfigurationProperties.DEFAULT_WORKER_COUNT.toInteger()
        config.getReadyTimeout() == Optional.empty()
        !config.getReadyTimeout().isPresent()
    }

    def "it resolves custom operator configuration properties"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.operator.ready-timeout": "20m",
                "kubernetes.client.operator.worker-count": 20
        ], Environment.KUBERNETES)

        when:
        def config = applicationContext.getBean(OperatorConfigurationProperties)

        then:
        config
        config.getReadyTimeout()
        config.getReadyTimeout().isPresent()
        config.getReadyTimeout().get() == Duration.parse("PT20m")
        config.getWorkerCount() == 20
    }
}
