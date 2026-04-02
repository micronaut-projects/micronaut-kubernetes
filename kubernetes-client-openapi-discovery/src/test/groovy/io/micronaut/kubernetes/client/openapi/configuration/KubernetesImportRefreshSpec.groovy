package io.micronaut.kubernetes.client.openapi.configuration

import io.micronaut.context.env.Environment
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import spock.lang.Specification

class KubernetesImportRefreshSpec extends Specification {

    void cleanup() {
        KubernetesConfigurationClient.propertySourceCache.clear()
    }

    void "config map watcher updates importer-loaded property source"() {
        given:
        def environment = Mock(Environment)
        environment.getPropertySourceLoaders() >> []
        environment.getProperty(_, String) >> Optional.empty()
        environment.refreshAndDiff() >> ["difficulty": "expert"]
        def publisher = Mock(ApplicationEventPublisher<RefreshEvent>)
        def watcher = new KubernetesConfigMapWatcher(environment, configMapConfiguration(), publisher)
        watcher.serviceStarted.set(true)
        def original = configMap('game-config', [difficulty: 'normal'])
        def updated = configMap('game-config', [difficulty: 'expert'], '2')
        def imported = watcher.readAsPropertySource(original)
        KubernetesConfigurationClient.addPropertySourceToCache(imported)

        when:
        watcher.onUpdate(original, updated)

        then:
        KubernetesConfigurationClient.propertySourceCache[imported.name].get('difficulty') == 'expert'
        1 * publisher.publishEvent(_ as RefreshEvent)
    }

    void "config map watcher skips unchanged resource version"() {
        given:
        def environment = Mock(Environment)
        environment.getPropertySourceLoaders() >> []
        environment.getProperty('v1configmap.game-config.resource-version', String) >> Optional.of('1')
        def publisher = Mock(ApplicationEventPublisher<RefreshEvent>)
        def watcher = new KubernetesConfigMapWatcher(environment, configMapConfiguration(), publisher)
        watcher.serviceStarted.set(true)
        def configMap = configMap('game-config', [difficulty: 'normal'], '1')
        def imported = watcher.readAsPropertySource(configMap)
        KubernetesConfigurationClient.addPropertySourceToCache(imported)

        when:
        watcher.onUpdate(configMap, configMap)

        then:
        KubernetesConfigurationClient.propertySourceCache[imported.name].get('difficulty') == 'normal'
        0 * publisher.publishEvent(_)
    }

    void "secret watcher removes importer-loaded property source on delete"() {
        given:
        def environment = Mock(Environment)
        environment.getProperty(_, String) >> Optional.empty()
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
        !KubernetesConfigurationClient.propertySourceCache.containsKey(imported.name)
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

    private static V1ConfigMap configMap(String name, Map<String, String> data, String resourceVersion = '1') {
        new V1ConfigMap()
            .metadata(new V1ObjectMeta().name(name).resourceVersion(resourceVersion))
            .data(data)
    }

    private static V1Secret secret(String name, Map<String, String> data, String resourceVersion = '1') {
        new V1Secret()
            .metadata(new V1ObjectMeta().name(name).resourceVersion(resourceVersion))
            .type(KubernetesSecretImportSupport.OPAQUE_SECRET_TYPE)
            .data(data.collectEntries { key, value -> [(key): value.bytes] })
    }
}
