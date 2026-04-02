package io.micronaut.kubernetes.imports

import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.PropertySourceImporter
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.convert.value.ConvertibleValues
import io.micronaut.core.util.ConnectionString
import io.micronaut.kubernetes.KubernetesConfiguration
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient
import io.micronaut.kubernetes.configuration.KubernetesConfigurationClient
import io.micronaut.kubernetes.configuration.KubernetesLegacyImportMode
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
        propertySource.get().get(KubernetesConfigurationClient.CONFIG_MAP_RESOURCE_VERSION) == '1'
    }

    void "label imports resolve first matching config map properties"() {
        given:
        String appliedSelector = null
        CoreV1ApiReactorClient.APIlistNamespacedConfigMapRequestReactive operation = Stub()
        operation.labelSelector(_) >> { String selector ->
            appliedSelector = selector
            operation
        }
        operation.execute() >> Mono.just(new V1ConfigMapList().items([
                configMap('selected-config', ['feature.enabled': 'true'])
        ]))
        CoreV1ApiReactorClient client = Stub() {
            listNamespacedConfigMap(_) >> operation
        }
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        def support = new KubernetesConfigMapImportSupport(client, configuration, new KubernetesLegacyImportMode())
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
        CoreV1ApiReactorClient.APIlistNamespacedConfigMapRequestReactive operation = Stub()
        operation.labelSelector(_) >> { String selector ->
            appliedSelector = selector
            operation
        }
        operation.execute() >> Mono.just(new V1ConfigMapList().items([]))
        CoreV1ApiReactorClient client = Stub() {
            listNamespacedConfigMap(_) >> operation
        }
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        def support = new KubernetesConfigMapImportSupport(client, configuration, new KubernetesLegacyImportMode())
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
        def importer = new KubernetesConfigMapPropertySourceImporter()

        when:
        def scalarResult = importer.newImportDeclaration(ConnectionString.parse('kubernetes-configmap://app-config'))
        def structuredResult = importer.newImportDeclaration(ConvertibleValues.of([labels: 'app=demo', namespace: 'team-b', optional: true]))

        then:
        importer.provider == 'kubernetes-configmap'
        scalarResult == new KubernetesConfigMapImport(null, 'app-config', null, false)
        structuredResult == new KubernetesConfigMapImport('team-b', null, [app: 'demo'], true)
    }

    void "service loader file lists classic importers exactly once and in order"() {
        when:
        def lines = ApplicationContext.classLoader
                .getResourceAsStream('META-INF/services/io.micronaut.context.env.PropertySourceImporter')
                .readLines()

        then:
        lines == [
                'io.micronaut.kubernetes.imports.KubernetesConfigMapPropertySourceImporter',
                'io.micronaut.kubernetes.imports.KubernetesSecretPropertySourceImporter'
        ]
    }

    void "explicit config map import disables same-type legacy bootstrap mode"() {
        given:
        def support = newSupportWithConfigMaps(configMap('game-config', [enemies: 'aliens']))
        def context = importContext(new KubernetesConfigMapImport('team-a', 'game-config', null, false))

        when:
        support.importPropertySource(context)

        then:
        !support.legacyImportMode.isLegacyBootstrapEnabled(KubernetesLegacyImportMode.LegacyType.CONFIG_MAP)
        support.legacyImportMode.isLegacyBootstrapEnabled(KubernetesLegacyImportMode.LegacyType.SECRET)
    }

    private KubernetesConfigMapImportSupport newSupport(String namespace = 'team-a') {
        def legacyImportMode = new KubernetesLegacyImportMode()
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> namespace
        }
        new KubernetesConfigMapImportSupport(Stub(CoreV1ApiReactorClient), configuration, legacyImportMode)
    }

    private KubernetesConfigMapImportSupport newSupportWithConfigMaps(V1ConfigMap... configMaps) {
        def legacyImportMode = new KubernetesLegacyImportMode()
        CoreV1ApiReactorClient.APIlistNamespacedConfigMapRequestReactive operation = Stub()
        operation.labelSelector(_) >> operation
        operation.execute() >> Mono.just(new V1ConfigMapList().items(configMaps.toList()))
        CoreV1ApiReactorClient client = Stub() {
            listNamespacedConfigMap(_) >> operation
        }
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        new KubernetesConfigMapImportSupport(client, configuration, legacyImportMode)
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
