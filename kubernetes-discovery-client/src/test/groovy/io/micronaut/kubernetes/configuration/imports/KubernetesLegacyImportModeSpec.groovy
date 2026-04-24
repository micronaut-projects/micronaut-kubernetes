package io.micronaut.kubernetes.configuration.imports

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.configuration.KubernetesConfigMapWatcher
import io.micronaut.kubernetes.configuration.KubernetesSecretWatcher
import spock.lang.Specification

class KubernetesLegacyImportModeSpec extends Specification {

    void cleanup() {
        KubernetesLegacyImportMode.reset()
    }

    void "explicit config map import disables legacy config map watcher"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'micronaut.config.import': 'optional:kubernetes://config-map?name=test'
        ], Environment.KUBERNETES)

        expect:
        !applicationContext.containsBean(KubernetesConfigMapWatcher)
    }

    void "explicit secret import disables legacy secret watcher"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'micronaut.config.import': 'optional:kubernetes://secret?name=test'
        ], Environment.KUBERNETES)

        expect:
        !applicationContext.containsBean(KubernetesSecretWatcher)
    }
}
