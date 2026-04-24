package io.micronaut.kubernetes.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Specification

class KubernetesConfigMapWatcherSpec extends Specification {

    void "KubernetesConfigMapWatcher exists when informer is enabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(Environment.KUBERNETES)

        expect:
        applicationContext.containsBean(KubernetesConfigMapWatcher)
    }

    void "KubernetesConfigMapWatcher is explicitly disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.config-maps.watch": "false"], Environment.KUBERNETES)

        expect:
        !applicationContext.containsBean(KubernetesConfigMapWatcher)
    }

    void "KubernetesConfigMapWatcher is disabled when config-client is disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["micronaut.config-client.enabled": "false"], Environment.KUBERNETES)

        expect:
        !applicationContext.containsBean(KubernetesConfigMapWatcher)
    }

    void "KubernetesConfigMapWatcher is disabled when mounted volume paths are specified"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.config-maps.paths": ["path1"]], Environment.KUBERNETES)

        expect:
        !applicationContext.containsBean(KubernetesConfigMapWatcher)
    }

    void "KubernetesConfigMapWatcher is enabled when mounted volume paths are specified and use-api is enabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.config-maps.paths"  : ["path1"],
                "kubernetes.client.config-maps.use-api": true
        ], Environment.KUBERNETES)

        expect:
        applicationContext.containsBean(KubernetesConfigMapWatcher)
    }
}
