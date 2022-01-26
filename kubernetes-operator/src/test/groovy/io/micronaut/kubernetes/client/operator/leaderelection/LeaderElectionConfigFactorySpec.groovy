package io.micronaut.kubernetes.client.operator.leaderelection

import io.kubernetes.client.extended.leaderelection.LeaderElectionConfig
import io.micronaut.context.ApplicationContext
import io.micronaut.kubernetes.client.operator.configuration.LeaderElectionConfigurationProperties
import spock.lang.Specification

import java.time.Duration

class LeaderElectionConfigFactorySpec extends Specification {

    def "it is created with default values"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run(
                ["micronaut.application.name": "app"]
        )

        then:
        applicationContext.getBean(LeaderElectionConfig)

        when:
        def config = applicationContext.getBean(LeaderElectionConfig)

        then:
        config.getLeaseDuration() == Duration.parse("PT" + LeaderElectionConfigurationProperties.DEFAULT_LEASE_DURATION_IN_SECONDS + "s")
        config.getRenewDeadline() == Duration.parse("PT" + LeaderElectionConfigurationProperties.DEFAULT_RENEW_DEADLINE_IN_SECONDS + "s")
        config.getRetryPeriod() == Duration.parse("PT" + LeaderElectionConfigurationProperties.DEFAULT_RETRY_PERIOD_IN_SECONDS + "s")

        and: 'default lock'
        config.getLock()

        and: 'empty owner reference'
        !config.getOwnerReference()
    }
}
