package io.micronaut.kubernetes.configuration

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires
import spock.lang.Specification

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "spec.reuseNamespace", value = "false")
class KubernetesSecretWatcherSpec extends Specification {

    void "KubernetesSecretWatcher not exists when informer is enabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(Environment.KUBERNETES)

        expect:
        !applicationContext.containsBean(KubernetesSecretWatcher)
    }

    void "KubernetesSecretWatcher is explicitly enabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.secrets.watch": "true"], Environment.KUBERNETES)

        expect:
        applicationContext.containsBean(KubernetesSecretWatcher)
    }

    void "KubernetesSecretWatcher is disabled when config-client is disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["micronaut.config-client.enabled": "false"], Environment.KUBERNETES)

        expect:
        !applicationContext.containsBean(KubernetesSecretWatcher)
    }

    void "KubernetesSecretWatcher is disabled when mounted volume paths are specified"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["kubernetes.client.secrets.paths": ["path1"]], Environment.KUBERNETES)

        expect:
        !applicationContext.containsBean(KubernetesSecretWatcher)
    }

    void "KubernetesSecretWatcher is enabled when mounted volume paths are specified and use-api is enabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.secrets.paths"  : ["path1"],
                "kubernetes.client.secrets.use-api": true
        ], Environment.KUBERNETES)

        expect:
        applicationContext.containsBean(KubernetesSecretWatcher)
    }
}
