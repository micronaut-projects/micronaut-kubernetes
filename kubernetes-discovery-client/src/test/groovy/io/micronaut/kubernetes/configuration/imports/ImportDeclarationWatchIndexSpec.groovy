package io.micronaut.kubernetes.configuration.imports

import spock.lang.Specification

class ImportDeclarationWatchIndexSpec extends Specification {

    void cleanup() {
        ImportDeclarationWatchIndex.reset()
    }

    void "add config map name declaration enables watcher and removes matching declaration"() {
        given:
        def declaration = new ImportDeclaration('config-map', 'app-config', null, null, true, false, false)

        when:
        ImportDeclarationWatchIndex.addConfigMapNameDeclaration('app-config', declaration)

        then:
        ImportDeclarationWatchIndex.isConfigMapWatcherEnabled()

        when:
        def removed = ImportDeclarationWatchIndex.removeConfigMapDeclarations('app-config', [:])

        then:
        removed == [declaration]
        ImportDeclarationWatchIndex.removeConfigMapDeclarations('app-config', [:]).isEmpty()
    }

    void "add config map labels declaration enables watcher and removes matching declaration"() {
        given:
        def declaration = new ImportDeclaration('config-map', null, [app: 'demo'], null, true, false, false)

        when:
        ImportDeclarationWatchIndex.addConfigMapLabelsDeclaration([app: 'demo'], declaration)

        then:
        ImportDeclarationWatchIndex.isConfigMapWatcherEnabled()

        when:
        def removed = ImportDeclarationWatchIndex.removeConfigMapDeclarations('other-name', [app: 'demo', env: 'test'])

        then:
        removed == [declaration]
        ImportDeclarationWatchIndex.removeConfigMapDeclarations('other-name', [app: 'demo', env: 'test']).isEmpty()
    }

    void "remove config map declarations returns name and label matches"() {
        given:
        def nameDeclaration = new ImportDeclaration('config-map', 'app-config', null, null, true, false, false)
        def labelsDeclaration = new ImportDeclaration('config-map', null, [app: 'demo'], null, true, false, false)
        def otherDeclaration = new ImportDeclaration('config-map', 'other-config', null, null, true, false, false)
        ImportDeclarationWatchIndex.addConfigMapNameDeclaration('app-config', nameDeclaration)
        ImportDeclarationWatchIndex.addConfigMapLabelsDeclaration([app: 'demo'], labelsDeclaration)
        ImportDeclarationWatchIndex.addConfigMapNameDeclaration('other-config', otherDeclaration)

        when:
        def removed = ImportDeclarationWatchIndex.removeConfigMapDeclarations('app-config', [app: 'demo', env: 'test'])

        then:
        removed.containsAll([nameDeclaration, labelsDeclaration])
        removed.size() == 2
        ImportDeclarationWatchIndex.removeConfigMapDeclarations('other-config', [:]) == [otherDeclaration]
    }

    void "add secret name declaration enables watcher and removes matching declaration"() {
        given:
        def declaration = new ImportDeclaration('secret', 'db-credentials', null, null, true, false, false)

        when:
        ImportDeclarationWatchIndex.addSecretNameDeclaration('db-credentials', declaration)

        then:
        ImportDeclarationWatchIndex.isSecretWatcherEnabled()

        when:
        def removed = ImportDeclarationWatchIndex.removeSecretDeclarations('db-credentials', [:])

        then:
        removed == [declaration]
        ImportDeclarationWatchIndex.removeSecretDeclarations('db-credentials', [:]).isEmpty()
    }

    void "add secret labels declaration enables watcher and removes matching declaration"() {
        given:
        def declaration = new ImportDeclaration('secret', null, [app: 'demo'], null, true, false, false)

        when:
        ImportDeclarationWatchIndex.addSecretLabelsDeclaration([app: 'demo'], declaration)

        then:
        ImportDeclarationWatchIndex.isSecretWatcherEnabled()

        when:
        def removed = ImportDeclarationWatchIndex.removeSecretDeclarations('other-name', [app: 'demo', env: 'test'])

        then:
        removed == [declaration]
        ImportDeclarationWatchIndex.removeSecretDeclarations('other-name', [app: 'demo', env: 'test']).isEmpty()
    }

    void "remove secret declarations returns name and label matches"() {
        given:
        def nameDeclaration = new ImportDeclaration('secret', 'db-credentials', null, null, true, false, false)
        def labelsDeclaration = new ImportDeclaration('secret', null, [app: 'demo'], null, true, false, false)
        def otherDeclaration = new ImportDeclaration('secret', 'other-secret', null, null, true, false, false)
        ImportDeclarationWatchIndex.addSecretNameDeclaration('db-credentials', nameDeclaration)
        ImportDeclarationWatchIndex.addSecretLabelsDeclaration([app: 'demo'], labelsDeclaration)
        ImportDeclarationWatchIndex.addSecretNameDeclaration('other-secret', otherDeclaration)

        when:
        def removed = ImportDeclarationWatchIndex.removeSecretDeclarations('db-credentials', [app: 'demo', env: 'test'])

        then:
        removed.containsAll([nameDeclaration, labelsDeclaration])
        removed.size() == 2
        ImportDeclarationWatchIndex.removeSecretDeclarations('other-secret', [:]) == [otherDeclaration]
    }

    void "remove methods ignore non matching objects and empty labels"() {
        given:
        def configMapDeclaration = new ImportDeclaration('config-map', null, [app: 'demo'], null, true, false, false)
        def secretDeclaration = new ImportDeclaration('secret', null, [app: 'demo'], null, true, false, false)
        ImportDeclarationWatchIndex.addConfigMapLabelsDeclaration([app: 'demo'], configMapDeclaration)
        ImportDeclarationWatchIndex.addSecretLabelsDeclaration([app: 'demo'], secretDeclaration)

        expect:
        ImportDeclarationWatchIndex.removeConfigMapDeclarations('other-name', [:]).isEmpty()
        ImportDeclarationWatchIndex.removeSecretDeclarations('other-name', [:]).isEmpty()
        ImportDeclarationWatchIndex.removeConfigMapDeclarations('other-name', [env: 'test']).isEmpty()
        ImportDeclarationWatchIndex.removeSecretDeclarations('other-name', [env: 'test']).isEmpty()
    }

    void "reset clears indexes and disables watchers"() {
        given:
        def configMapDeclaration = new ImportDeclaration('config-map', 'app-config', null, null, true, false, false)
        def secretDeclaration = new ImportDeclaration('secret', 'db-credentials', null, null, true, false, false)
        ImportDeclarationWatchIndex.addConfigMapNameDeclaration('app-config', configMapDeclaration)
        ImportDeclarationWatchIndex.addSecretNameDeclaration('db-credentials', secretDeclaration)

        when:
        ImportDeclarationWatchIndex.reset()

        then:
        !ImportDeclarationWatchIndex.isConfigMapWatcherEnabled()
        !ImportDeclarationWatchIndex.isSecretWatcherEnabled()
        ImportDeclarationWatchIndex.removeConfigMapDeclarations('app-config', [:]).isEmpty()
        ImportDeclarationWatchIndex.removeSecretDeclarations('db-credentials', [:]).isEmpty()
    }
}
