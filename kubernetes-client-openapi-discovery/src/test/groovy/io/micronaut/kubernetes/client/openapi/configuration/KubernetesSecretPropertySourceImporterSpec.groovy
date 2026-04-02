package io.micronaut.kubernetes.client.openapi.configuration

import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.context.env.PropertySourceImporter
import io.micronaut.core.convert.value.ConvertibleValues
import io.micronaut.core.util.ConnectionString
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.model.V1Secret
import io.micronaut.kubernetes.client.openapi.model.V1SecretList
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
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

        when:
        def withOverride = support.newImportDeclaration(ConnectionString.parse('kubernetes-secret://db-credentials?namespace=shared'))
        def withDefault = support.newImportDeclaration(ConnectionString.parse('kubernetes-secret://db-credentials'))
        def labelImport = support.newImportDeclaration(ConvertibleValues.of([labels: 'app=my-app', namespace: 'ops']))

        then:
        withOverride.namespace() == 'shared'
        withDefault.namespace() == 'default-namespace'
        labelImport.namespace() == 'ops'
        labelImport.labels() == [app: 'my-app']
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
        String appliedSelector = null
        CoreV1ApiReactor client = Mock()
        client.listNamespacedSecret(_, _, _, _, _, _, _, _, _, _, _, _) >> { args ->
            appliedSelector = args[5]
            Mono.just(new V1SecretList([
                secret('tls-secret', 'kubernetes.io/tls', [token: 'ignored']),
                secret('opaque-secret', KubernetesSecretImportSupport.OPAQUE_SECRET_TYPE, [token: 'selected'])
            ]))
        }
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        def support = new KubernetesSecretImportSupport(client, configuration)
        def context = importContext(new KubernetesSecretImport('team-a', null, [app: 'demo'], false))

        when:
        def propertySource = support.importPropertySource(context)

        then:
        propertySource.present
        propertySource.get().get('token') == 'selected'
        appliedSelector == 'app=demo'
    }

    void "zero-result label secret imports are skipped"() {
        given:
        String appliedSelector = null
        CoreV1ApiReactor client = Mock()
        client.listNamespacedSecret(_, _, _, _, _, _, _, _, _, _, _, _) >> { args ->
            appliedSelector = args[5]
            Mono.just(new V1SecretList([]))
        }
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        def support = new KubernetesSecretImportSupport(client, configuration)
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

        when:
        def declaration = support.newImportDeclaration(ConvertibleValues.of([labels: 'app=demo, , env=test']))

        then:
        declaration.labels() == [app: 'demo', env: 'test']
    }

    void "importer exposes provider and uses support parsing methods"() {
        given:
        def support = newSupport('team-a')
        def importer = new KubernetesSecretPropertySourceImporter(support)

        when:
        def scalarResult = importer.newImportDeclaration(ConnectionString.parse('kubernetes-secret://db-credentials'))
        def structuredResult = importer.newImportDeclaration(ConvertibleValues.of([labels: 'app=demo', namespace: 'team-b', optional: true]))

        then:
        importer.provider == 'kubernetes-secret'
        scalarResult == new KubernetesSecretImport('team-a', 'db-credentials', null, false)
        structuredResult == new KubernetesSecretImport('team-b', null, [app: 'demo'], true)
    }

    private KubernetesSecretImportSupport newSupport(String namespace = 'team-a') {
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> namespace
        }
        new KubernetesSecretImportSupport(Stub(CoreV1ApiReactor), configuration)
    }

    private KubernetesSecretImportSupport newSupportWithSecrets(V1Secret... secrets) {
        CoreV1ApiReactor client = Mock()
        client.listNamespacedSecret(_, _, _, _, _, _, _, _, _, _, _, _) >> Mono.just(new V1SecretList(secrets.toList()))
        KubernetesConfiguration configuration = Stub() {
            getNamespace() >> 'team-a'
        }
        new KubernetesSecretImportSupport(client, configuration)
    }

    private PropertySourceImporter.ImportContext<KubernetesSecretImport> importContext(KubernetesSecretImport declaration) {
        Stub(PropertySourceImporter.ImportContext) {
            importDeclaration() >> declaration
        }
    }

    private static V1Secret secret(String name, String type, Map<String, String> data) {
        new V1Secret()
            .metadata(new V1ObjectMeta().name(name).resourceVersion('1'))
            .type(type)
            .data(data.collectEntries { key, value -> [(key): value.bytes] })
    }
}
