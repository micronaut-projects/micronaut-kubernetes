package io.micronaut.kubernetes.client.operator.configuration

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

import java.time.Duration

class LeaderElectionConfigurationPropertiesSpec extends Specification {

    def "it resolves leader election configuration defaults"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run()

        then:
        applicationContext.getBean(LeaderElectionConfiguration)

        when:
        def config = applicationContext.getBean(LeaderElectionConfiguration)

        then:
        config.getResourceName() == Optional.empty()
        config.getResourceNamespace() == Optional.empty()
        config.getLeaseDuration() == Duration.parse("PT" + LeaderElectionConfigurationProperties.DEFAULT_LEASE_DURATION_IN_SECONDS + "s")
        config.getRenewDeadline() == Duration.parse("PT" + LeaderElectionConfigurationProperties.DEFAULT_RENEW_DEADLINE_IN_SECONDS + "s")
        config.getRetryPeriod() == Duration.parse("PT" + LeaderElectionConfigurationProperties.DEFAULT_RETRY_PERIOD_IN_SECONDS + "s")
    }

    def "it resolves custom leader election configuration properties"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.operator.leader-election.lock.resource-name": "custom-name",
                "kubernetes.client.operator.leader-election.lock.resource-namespace": "custom-namespace",
                "kubernetes.client.operator.leader-election.lock.lease-duration": "20m",
                "kubernetes.client.operator.leader-election.lock.renew-deadline": "20m",
                "kubernetes.client.operator.leader-election.lock.retry-period": "20m"])

        when:
        def lockConfig = applicationContext.getBean(LeaderElectionConfiguration)

        then:
        lockConfig.getResourceName()
        lockConfig.getResourceName().get() == "custom-name"
        lockConfig.getResourceNamespace()
        lockConfig.getResourceNamespace().get() == "custom-namespace"
        lockConfig.getLeaseDuration() == Duration.parse("PT20m")
        lockConfig.getRenewDeadline() == Duration.parse("PT20m")
        lockConfig.getRetryPeriod() == Duration.parse("PT20m")
    }
}
