package io.micronaut.kubernetes.configuration.imports

import spock.lang.Specification

class ImportDeclarationWatchIndexSpec extends Specification {

    void cleanup() {
        ImportDeclarationWatchIndex.reset()
    }

    void "increments refresh count for watched config map by name"() {
        given:
        def declaration = new ImportDeclaration('config-map', 'orders', null, null, true, false, false)
        ImportDeclarationWatchIndex.addConfigMapNameDeclaration('orders', declaration)

        expect:
        ImportDeclarationWatchIndex.isConfigMapWatcherEnabled()

        when:
        def watched = ImportDeclarationWatchIndex.updateRefreshCountIfConfigMapWatched('orders', [:])

        then:
        watched
        ImportDeclarationWatchIndex.getRefreshCount(declaration).get() == 1
    }

    void "increments refresh count for watched config map by labels"() {
        given:
        def declaration = new ImportDeclaration('config-map', null, [app: 'demo', env: 'test'], null, true, false, false)
        ImportDeclarationWatchIndex.addConfigMapLabelsDeclaration([app: 'demo', env: 'test'], declaration)

        when:
        def watched = ImportDeclarationWatchIndex.updateRefreshCountIfConfigMapWatched('orders', [app: 'demo', env: 'test', tier: 'backend'])

        then:
        watched
        ImportDeclarationWatchIndex.getRefreshCount(declaration).get() == 1
    }

    void "increments refresh count for watched secret by name and labels"() {
        given:
        def nameDeclaration = new ImportDeclaration('secret', 'db-credentials', null, null, true, false, false)
        def labelsDeclaration = new ImportDeclaration('secret', null, [app: 'demo'], null, true, false, false)
        ImportDeclarationWatchIndex.addSecretNameDeclaration('db-credentials', nameDeclaration)
        ImportDeclarationWatchIndex.addSecretLabelsDeclaration([app: 'demo'], labelsDeclaration)

        expect:
        ImportDeclarationWatchIndex.isSecretWatcherEnabled()

        when:
        def matchedByName = ImportDeclarationWatchIndex.updateRefreshCountIfSecretWatched('db-credentials', [:])
        def matchedByLabels = ImportDeclarationWatchIndex.updateRefreshCountIfSecretWatched('other-secret', [app: 'demo', env: 'test'])

        then:
        matchedByName
        matchedByLabels
        ImportDeclarationWatchIndex.getRefreshCount(nameDeclaration).get() == 1
        ImportDeclarationWatchIndex.getRefreshCount(labelsDeclaration).get() == 1
    }

    void "does not increment refresh count when resource is not watched"() {
        given:
        def declaration = new ImportDeclaration('config-map', 'orders', null, null, true, false, false)
        ImportDeclarationWatchIndex.addConfigMapNameDeclaration('orders', declaration)

        when:
        def watched = ImportDeclarationWatchIndex.updateRefreshCountIfConfigMapWatched('inventory', [app: 'demo'])

        then:
        !watched
        ImportDeclarationWatchIndex.getRefreshCount(declaration).get() == 0
    }

    void "reset clears indexes counts and watcher flags"() {
        given:
        def configMapDeclaration = new ImportDeclaration('config-map', 'orders', null, null, true, false, false)
        def secretDeclaration = new ImportDeclaration('secret', null, [app: 'demo'], null, true, false, false)
        ImportDeclarationWatchIndex.addConfigMapNameDeclaration('orders', configMapDeclaration)
        ImportDeclarationWatchIndex.addSecretLabelsDeclaration([app: 'demo'], secretDeclaration)
        ImportDeclarationWatchIndex.updateRefreshCountIfConfigMapWatched('orders', [:])
        ImportDeclarationWatchIndex.updateRefreshCountIfSecretWatched('other-secret', [app: 'demo'])

        when:
        ImportDeclarationWatchIndex.reset()

        then:
        !ImportDeclarationWatchIndex.isConfigMapWatcherEnabled()
        !ImportDeclarationWatchIndex.isSecretWatcherEnabled()
        ImportDeclarationWatchIndex.getRefreshCount(configMapDeclaration) == null
        ImportDeclarationWatchIndex.getRefreshCount(secretDeclaration) == null
        !ImportDeclarationWatchIndex.updateRefreshCountIfConfigMapWatched('orders', [:])
        !ImportDeclarationWatchIndex.updateRefreshCountIfSecretWatched('other-secret', [app: 'demo'])
    }
}
