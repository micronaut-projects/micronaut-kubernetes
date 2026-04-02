package io.micronaut.kubernetes.client.openapi.configuration

import io.micronaut.context.env.PropertySourceImporter
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.convert.value.ConvertibleValues
import io.micronaut.core.util.ConnectionString
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMap
import io.micronaut.kubernetes.client.openapi.model.V1ConfigMapList
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import reactor.core.publisher.Mono
import spock.lang.Specification

class KubernetesConfigMapPropertySourceImporterSpec extends Specification {

    void "it rejects declarations with both path and labels"() {
        given:
        def support = newSupport()

        when:
        support.newImportDeclaration(ConnectionString.parse('kubernetes-configmap://test-map?labels=app=test-map'))

        then:
        ConfigurationException e = thrown()
        e.message.contains('requires exactly one selector')
    }

    void "it rejects declarations with neither path nor labels"() {
        given:
        def support = newSupport()

        when:
        support.newImportDeclaration(ConvertibleValues.of([namespace: 'team-a']))

        then:
        ConfigurationException e = thrown()
        e.message.contains('requires exactly one selector')
    }

    void "it resolves namespace from connection string and defaults from configuration"() {
        given:
        def support = newSupport('default-namespace')

        when:
        def withOverride = support.newImportDeclaration(ConnectionString.parse('kubernetes-configmap://app-config?namespace=shared'))
        def withDefault = support.newImportDeclaration(ConnectionString.parse('kubernetes-configmap://app-config'))
        def labelImport = support.newImportDeclaration(ConvertibleValues.of([labels: 'app=my-app', namespace: 'ops']))

        then:
        withOverride.namespace() == 'shared'
        withDefault.namespace() == 'default-namespace'
        labelImport.namespace() == 'ops'
        labelImport.labels() == [app: 'my-app']
    }

    void "exact-name imports resolve config map properties"() {
        given:
        def support = newSupportWithConfigMaps(configMap('game-config', [
            enemies: 'aliens',
            lives: '3',
            'special.level': 'expert'
        ]))
        def context = importContext(new KubernetesConfigMapImport('team-a', 'game-config', null, false))

        when:
        def propertySource = support.importPropertySource(context)

        then:
        propertySource.present
        propertySource.get().get('enemies') == 'aliens'
        propertySource.get().get('lives') == '3'
        propertySource.get().get('special.level') == 'expert'
        propertySource.get().get('v1configmap.game-config.resource-version') == '1'
    }

    void "label imports resolve first matching config map properties"() {
        given:
        String appliedSelector = null
        CoreV1ApiReactor client = Mock()
        client.listNamespacedConfigMap(_, _, _, _, _, _, _, _, _, _, _, _) >> { args ->
            appliedSelector = args[5]
            Mono.just(new V1ConfigMapList([
                configMap('selected-config', ['feature.enabled': 'true'])
            ]))
        }
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        def support = new KubernetesConfigMapImportSupport(client, configuration)
        def context = importContext(new KubernetesConfigMapImport('team-a', null, [app: 'demo', env: 'test'], false))

        when:
        def propertySource = support.importPropertySource(context)

        then:
        propertySource.present
        appliedSelector == 'app=demo,env=test'
    }

    void "zero-result label imports are skipped"() {
        given:
        String appliedSelector = null
        CoreV1ApiReactor client = Mock()
        client.listNamespacedConfigMap(_, _, _, _, _, _, _, _, _, _, _, _) >> { args ->
            appliedSelector = args[5]
            Mono.just(new V1ConfigMapList([]))
        }
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        def support = new KubernetesConfigMapImportSupport(client, configuration)
        def context = importContext(new KubernetesConfigMapImport('team-a', null, [app: 'demo'], true))

        when:
        def propertySource = support.importPropertySource(context)

        then:
        !propertySource.present
        appliedSelector == 'app=demo'
    }

    void "blank label entries are ignored when parsing labels"() {
        given:
        def support = newSupport()

        when:
        def declaration = support.newImportDeclaration(ConvertibleValues.of([labels: 'app=demo, , env=test']))

        then:
        declaration.labels() == [app: 'demo', env: 'test']
    }

    void "optional missing config map imports are skipped"() {
        given:
        def support = newSupportWithConfigMaps()
        def context = importContext(new KubernetesConfigMapImport('team-a', 'missing-config', null, true))

        expect:
        !support.importPropertySource(context).present
    }

    void "explicit namespace and default namespace produce distinct declarations"() {
        given:
        def support = newSupport('team-a')

        when:
        def explicitNamespace = support.newImportDeclaration(ConnectionString.parse('kubernetes-configmap://app-config?namespace=shared'))
        def defaultNamespace = support.newImportDeclaration(ConnectionString.parse('kubernetes-configmap://app-config'))

        then:
        explicitNamespace != defaultNamespace
        explicitNamespace.namespace() == 'shared'
        defaultNamespace.namespace() == 'team-a'
    }

    void "importer exposes provider and uses support parsing methods"() {
        given:
        def support = newSupport('team-a')
        def importer = new KubernetesConfigMapPropertySourceImporter(support)

        when:
        def scalarResult = importer.newImportDeclaration(ConnectionString.parse('kubernetes-configmap://app-config'))
        def structuredResult = importer.newImportDeclaration(ConvertibleValues.of([labels: 'app=demo', namespace: 'team-b', optional: true]))

        then:
        importer.provider == 'kubernetes-configmap'
        scalarResult == new KubernetesConfigMapImport('team-a', 'app-config', null, false)
        structuredResult == new KubernetesConfigMapImport('team-b', null, [app: 'demo'], true)
    }

    private KubernetesConfigMapImportSupport newSupport(String namespace = 'team-a') {
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> namespace
        }
        new KubernetesConfigMapImportSupport(Stub(CoreV1ApiReactor), configuration)
    }

    private KubernetesConfigMapImportSupport newSupportWithConfigMaps(V1ConfigMap... configMaps) {
        CoreV1ApiReactor client = Mock()
        client.listNamespacedConfigMap(_, _, _, _, _, _, _, _, _, _, _, _) >> Mono.just(new V1ConfigMapList(configMaps.toList()))
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        new KubernetesConfigMapImportSupport(client, configuration)
    }

    private PropertySourceImporter.ImportContext<KubernetesConfigMapImport> importContext(KubernetesConfigMapImport declaration) {
        def environment = Stub(io.micronaut.context.env.Environment) {
            getPropertySourceLoaders() >> []
        }
        Stub(PropertySourceImporter.ImportContext) {
            importDeclaration() >> declaration
            environment() >> environment
        }
    }

    private static V1ConfigMap configMap(String name, Map<String, String> data) {
        new V1ConfigMap()
            .metadata(new V1ObjectMeta().name(name).resourceVersion('1'))
            .data(data)
    }
}
