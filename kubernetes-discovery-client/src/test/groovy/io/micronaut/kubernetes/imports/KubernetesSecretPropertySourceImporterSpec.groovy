package io.micronaut.kubernetes.imports

import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1Secret
import io.kubernetes.client.openapi.models.V1SecretList
import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.context.env.PropertySourceImporter
import io.micronaut.core.convert.value.ConvertibleValues
import io.micronaut.core.util.ConnectionString
import io.micronaut.kubernetes.KubernetesConfiguration
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient
import io.micronaut.kubernetes.configuration.KubernetesConfigurationClient
import io.micronaut.kubernetes.configuration.KubernetesLegacyImportMode
import reactor.core.publisher.Mono
import spock.lang.Specification

class KubernetesSecretPropertySourceImporterSpec extends Specification {

    void "it rejects declarations with both path and labels"() {
        given:
        def support = newSupport()

        when:
        support.newImportDeclaration(ConnectionString.parse('kubernetes-secret://test-secret?labels=app=test-secret'))

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
        def importer = new KubernetesSecretPropertySourceImporter()

        when:
        def withOverride = support.newImportDeclaration(ConnectionString.parse('kubernetes-secret://db-credentials?namespace=shared'))
        def withDefault = support.newImportDeclaration(ConnectionString.parse('kubernetes-secret://db-credentials'))
        def labelImport = support.newImportDeclaration(ConvertibleValues.of([labels: 'app=my-app', namespace: 'ops']))
        def importerDeclaration = importer.newImportDeclaration(ConnectionString.parse('kubernetes-secret://db-credentials'))

        then:
        withOverride.namespace() == 'shared'
        withDefault.namespace() == 'default-namespace'
        labelImport.namespace() == 'ops'
        labelImport.labels() == [app: 'my-app']
        importerDeclaration == new KubernetesSecretImport(null, 'db-credentials', null, false)
    }

    void "required non-opaque exact-name imports fail"() {
        given:
        def support = newSupportWithSecrets(secret('db-credentials', 'kubernetes.io/tls', [token: 'ignored']))
        def context = importContext(new KubernetesSecretImport('team-a', 'db-credentials', null, false))

        when:
        support.importPropertySource(context)

        then:
        ConfigurationException e = thrown()
        e.message.contains('to be of type [Opaque]')
    }

    void "optional non-opaque exact-name imports are skipped"() {
        given:
        def support = newSupportWithSecrets(secret('db-credentials', 'kubernetes.io/tls', [token: 'ignored']))
        def context = importContext(new KubernetesSecretImport('team-a', 'db-credentials', null, true))

        expect:
        !support.importPropertySource(context).present
    }

    void "label imports ignore non-opaque matches"() {
        given:
        def support = newSupportWithSecrets(
                secret('tls-secret', 'kubernetes.io/tls', [token: 'ignored']),
                secret('opaque-secret', KubernetesConfigurationClient.OPAQUE_SECRET_TYPE, [token: 'selected'])
        )
        def context = importContext(new KubernetesSecretImport('team-a', null, [app: 'demo'], false))

        when:
        def propertySource = support.importPropertySource(context)

        then:
        propertySource.present
        propertySource.get().get('token') == 'selected'
    }

    void "zero-result label secret imports are skipped"() {
        given:
        String appliedSelector = null
        CoreV1ApiReactorClient.APIlistNamespacedSecretRequestReactive operation = Stub()
        operation.labelSelector(_) >> { String selector ->
            appliedSelector = selector
            operation
        }
        operation.execute() >> Mono.just(new V1SecretList().items([]))
        CoreV1ApiReactorClient client = Stub() {
            listNamespacedSecret(_) >> operation
        }
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        def support = new KubernetesSecretImportSupport(client, configuration, new KubernetesLegacyImportMode())
        def context = importContext(new KubernetesSecretImport('team-a', null, [app: 'demo'], true))

        when:
        def propertySource = support.importPropertySource(context)

        then:
        !propertySource.present
        appliedSelector == 'app=demo'
    }

    void "blank label entries are ignored when parsing labels"() {
        given:
        def support = newSupport()
        def importer = new KubernetesSecretPropertySourceImporter()

        when:
        def declaration = support.newImportDeclaration(ConvertibleValues.of([labels: 'app=demo, , env=test']))
        def importerDeclaration = importer.newImportDeclaration(ConvertibleValues.of([labels: 'app=demo, , env=test']))

        then:
        declaration.labels() == [app: 'demo', env: 'test']
        importerDeclaration.labels() == [app: 'demo', env: 'test']
    }

    void "importer exposes provider and uses support parsing methods"() {
        given:
        def importer = new KubernetesSecretPropertySourceImporter()

        when:
        def scalarResult = importer.newImportDeclaration(ConnectionString.parse('kubernetes-secret://db-credentials'))
        def structuredResult = importer.newImportDeclaration(ConvertibleValues.of([labels: 'app=demo', namespace: 'team-b', optional: true]))

        then:
        importer.provider == 'kubernetes-secret'
        scalarResult == new KubernetesSecretImport(null, 'db-credentials', null, false)
        structuredResult == new KubernetesSecretImport('team-b', null, [app: 'demo'], true)
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

    void "explicit secret import disables same-type legacy bootstrap mode"() {
        given:
        def support = newSupportWithSecrets(secret('db-credentials', KubernetesConfigurationClient.OPAQUE_SECRET_TYPE, [token: 'selected']))
        def context = importContext(new KubernetesSecretImport('team-a', 'db-credentials', null, false))

        when:
        support.importPropertySource(context)

        then:
        !support.legacyImportMode.isLegacyBootstrapEnabled(KubernetesLegacyImportMode.LegacyType.SECRET)
        support.legacyImportMode.isLegacyBootstrapEnabled(KubernetesLegacyImportMode.LegacyType.CONFIG_MAP)
    }

    private KubernetesSecretImportSupport newSupport(String namespace = 'team-a') {
        def legacyImportMode = new KubernetesLegacyImportMode()
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> namespace
        }
        new KubernetesSecretImportSupport(Stub(CoreV1ApiReactorClient), configuration, legacyImportMode)
    }

    private KubernetesSecretImportSupport newSupportWithSecrets(V1Secret... secrets) {
        def legacyImportMode = new KubernetesLegacyImportMode()
        CoreV1ApiReactorClient.APIlistNamespacedSecretRequestReactive operation = Stub()
        operation.labelSelector(_) >> operation
        operation.execute() >> Mono.just(new V1SecretList().items(secrets.toList()))
        CoreV1ApiReactorClient client = Stub() {
            listNamespacedSecret(_) >> operation
        }
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        new KubernetesSecretImportSupport(client, configuration, legacyImportMode)
    }

    private PropertySourceImporter.ImportContext<KubernetesSecretImport> importContext(KubernetesSecretImport declaration) {
        Stub(PropertySourceImporter.ImportContext) {
            importDeclaration() >> declaration
        }
    }

    private static V1Secret secret(String name, String type, Map<String, String> data) {
        new V1Secret()
                .metadata(new V1ObjectMeta().name(name))
                .type(type)
                .data(data.collectEntries { key, value -> [(key): value.bytes] })
    }
}
