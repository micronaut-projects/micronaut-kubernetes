package io.micronaut.kubernetes.configuration

import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1Secret
import io.micronaut.context.env.Environment
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.kubernetes.KubernetesConfiguration
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import spock.lang.Specification

class KubernetesImportRefreshSpec extends Specification {

    void cleanup() {
        KubernetesConfigurationClient.getPropertySourceCache().clear()
    }

    void "config map watcher updates importer-loaded property source"() {
        given:
        def environment = Mock(Environment)
        environment.getPropertySourceLoaders() >> []
        environment.refreshAndDiff() >> ["difficulty": "expert"]
        def publisher = Mock(ApplicationEventPublisher<RefreshEvent>)
        def watcher = new KubernetesConfigMapWatcher(environment, configMapConfiguration(), publisher)
        watcher.serviceStarted.set(true)
        def original = configMap('game-config', [difficulty: 'normal'])
        def updated = configMap('game-config', [difficulty: 'expert'])
        def imported = watcher.readAsPropertySource(original)
        KubernetesConfigurationClient.addPropertySourceToCache(imported)

        when:
        watcher.onUpdate(original, updated)

        then:
        KubernetesConfigurationClient.getPropertySourceCache()[imported.name].get('difficulty') == 'expert'
        1 * publisher.publishEvent(_ as RefreshEvent)
    }

    void "config map watcher removes importer-loaded property source on delete"() {
        given:
        def environment = Mock(Environment)
        environment.getPropertySourceLoaders() >> []
        environment.refreshAndDiff() >> ["difficulty": null]
        def publisher = Mock(ApplicationEventPublisher<RefreshEvent>)
        def watcher = new KubernetesConfigMapWatcher(environment, configMapConfiguration(), publisher)
        watcher.serviceStarted.set(true)
        def configMap = configMap('game-config', [difficulty: 'normal'])
        def imported = watcher.readAsPropertySource(configMap)
        KubernetesConfigurationClient.addPropertySourceToCache(imported)

        when:
        watcher.onDelete(configMap, false)

        then:
        !KubernetesConfigurationClient.getPropertySourceCache().containsKey(imported.name)
        1 * publisher.publishEvent(_ as RefreshEvent)
    }

    void "secret watcher updates importer-loaded property source"() {
        given:
        def environment = Mock(Environment)
        environment.refreshAndDiff() >> ["token": "updated"]
        def publisher = Mock(ApplicationEventPublisher<RefreshEvent>)
        def watcher = new KubernetesSecretWatcher(environment, secretConfiguration(), publisher)
        watcher.serviceStarted.set(true)
        def original = secret('db-credentials', [token: 'initial'])
        def updated = secret('db-credentials', [token: 'updated'])
        def imported = watcher.readAsPropertySource(original)
        KubernetesConfigurationClient.addPropertySourceToCache(imported)

        when:
        watcher.onUpdate(original, updated)

        then:
        KubernetesConfigurationClient.getPropertySourceCache()[imported.name].get('token') == 'updated'
        1 * publisher.publishEvent(_ as RefreshEvent)
    }

    void "secret watcher removes importer-loaded property source on delete"() {
        given:
        def environment = Mock(Environment)
        environment.refreshAndDiff() >> ["token": null]
        def publisher = Mock(ApplicationEventPublisher<RefreshEvent>)
        def watcher = new KubernetesSecretWatcher(environment, secretConfiguration(), publisher)
        watcher.serviceStarted.set(true)
        def secret = secret('db-credentials', [token: 'initial'])
        def imported = watcher.readAsPropertySource(secret)
        KubernetesConfigurationClient.addPropertySourceToCache(imported)

        when:
        watcher.onDelete(secret, false)

        then:
        !KubernetesConfigurationClient.getPropertySourceCache().containsKey(imported.name)
        1 * publisher.publishEvent(_ as RefreshEvent)
    }

    void "watcher add stores importer-compatible config map property source name"() {
        given:
        def environment = Mock(Environment)
        environment.getPropertySourceLoaders() >> []
        environment.refreshAndDiff() >> ["feature.enabled": "true"]
        def publisher = Mock(ApplicationEventPublisher<RefreshEvent>)
        def watcher = new KubernetesConfigMapWatcher(environment, configMapConfiguration(), publisher)
        watcher.serviceStarted.set(true)
        def configMap = configMap('optional-config', [
            'feature.enabled': 'true',
            'feature.mode': 'demo'
        ])

        when:
        watcher.onAdd(configMap)

        then:
        KubernetesConfigurationClient.getPropertySourceCache().containsKey('optional-config (Kubernetes ConfigMap)')
        1 * publisher.publishEvent(_ as RefreshEvent)
    }

    private KubernetesConfiguration configMapConfiguration() {
        Stub(KubernetesConfiguration) {
            getConfigMaps() >> Stub(KubernetesConfiguration.KubernetesConfigMapsConfiguration) {
                getIncludes() >> []
                getExcludes() >> []
            }
        }
    }

    private KubernetesConfiguration secretConfiguration() {
        Stub(KubernetesConfiguration) {
            getSecrets() >> Stub(KubernetesConfiguration.KubernetesSecretsConfiguration) {
                getIncludes() >> []
                getExcludes() >> []
            }
        }
    }

    private static V1ConfigMap configMap(String name, Map<String, String> data) {
        new V1ConfigMap().metadata(new V1ObjectMeta().name(name).resourceVersion('1')).data(data)
    }

    private static V1Secret secret(String name, Map<String, String> data) {
        new V1Secret()
            .metadata(new V1ObjectMeta().name(name))
            .type(KubernetesConfigurationClient.OPAQUE_SECRET_TYPE)
            .data(data.collectEntries { key, value -> [(key): value.bytes] })
    }
}
