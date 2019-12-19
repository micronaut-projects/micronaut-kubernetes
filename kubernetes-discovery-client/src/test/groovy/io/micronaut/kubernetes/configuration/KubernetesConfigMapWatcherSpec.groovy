package io.micronaut.kubernetes.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Specification

class KubernetesConfigMapWatcherSpec extends Specification {

    void "KubernetesConfigMapWatcher exists by default"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(Environment.KUBERNETES)

        expect:
        applicationContext.containsBean(KubernetesConfigMapWatcher)
    }

    void "KubernetesConfigMapWatcher can be disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.config-maps.watch": "false"], Environment.KUBERNETES)

        expect:
        !applicationContext.containsBean(KubernetesConfigMapWatcher)

    }

}
