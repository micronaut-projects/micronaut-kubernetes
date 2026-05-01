package io.micronaut.kubernetes.configuration.imports

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.convert.value.ConvertibleValues
import io.micronaut.core.util.ConnectionString
import spock.lang.Specification

class KubernetesPropertySourceImporterSpec extends Specification {

    void "exposes kubernetes provider"() {
        expect:
        new KubernetesPropertySourceImporter().provider == 'kubernetes'
    }

    void "parses secret declarations from connection string"() {
        given:
        def importer = new KubernetesPropertySourceImporter()

        when:
        def declaration = importer.newImportDeclaration(ConnectionString.parse('kubernetes://secret?name=db-credentials&watch=false'))

        then:
        declaration == new ImportDeclaration('secret', 'db-credentials', null, null, false, false, false)
    }

    void "parses config map declarations from values"() {
        given:
        def importer = new KubernetesPropertySourceImporter()

        when:
        def declaration = importer.newImportDeclaration(ConvertibleValues.of([
                type                       : 'config-map',
                labels                     : 'app=demo, env=test',
                watch                      : false,
                exceptionOnPodLabelsMissing: true,
                terminateStartupOnException: true
        ]))

        then:
        declaration == new ImportDeclaration('config-map', null, [app: 'demo', env: 'test'], null, false, true, true)
    }

    void "rejects unsupported options"() {
        given:
        def importer = new KubernetesPropertySourceImporter()

        when:
        importer.newImportDeclaration(ConnectionString.parse('kubernetes://secret?name=db-credentials&namespace=team-a'))

        then:
        ConfigurationException e = thrown()
        e.message.contains('does not support options: [namespace]')
    }

    void "rejects declarations without selectors"() {
        given:
        def importer = new KubernetesPropertySourceImporter()

        when:
        importer.newImportDeclaration(ConnectionString.parse('kubernetes://config-map'))

        then:
        ConfigurationException e = thrown()
        e.message.contains("requires at least one selector")
    }

    void "rejects declarations with both name and labels"() {
        given:
        def importer = new KubernetesPropertySourceImporter()

        when:
        importer.newImportDeclaration(ConnectionString.parse('kubernetes://secret?name=db-credentials&labels=app=demo'))

        then:
        ConfigurationException e = thrown()
        e.message.contains("does not allow 'name' and 'labels' to be set at the same time")
    }

    void "rejects declarations with both name and pod labels"() {
        given:
        def importer = new KubernetesPropertySourceImporter()

        when:
        importer.newImportDeclaration(ConvertibleValues.of([
                type     : 'secret',
                name     : 'db-credentials',
                podLabels: 'app,env'
        ]))

        then:
        ConfigurationException e = thrown()
        e.message.contains("does not allow 'name' and 'podLabels' to be set at the same time")
    }

    void "parses pod labels into a trimmed list"() {
        given:
        def importer = new KubernetesPropertySourceImporter()

        when:
        def declaration = importer.newImportDeclaration(ConvertibleValues.of([
                type     : 'config-map',
                podLabels: ' app , , env '
        ]))

        then:
        declaration == new ImportDeclaration('config-map', null, null, ['app', 'env'], true, false, false)
    }

    void "parses watcher-related flags from connection string"() {
        given:
        def importer = new KubernetesPropertySourceImporter()

        when:
        def declaration = importer.newImportDeclaration(ConnectionString.parse('kubernetes://secret?labels=app=demo&watch=false&exceptionOnPodLabelsMissing=true&terminateStartupOnException=true'))

        then:
        declaration == new ImportDeclaration('secret', null, [app: 'demo'], null, false, true, true)
    }

    void "service loader file lists kubernetes importer"() {
        given:
        def lines = ApplicationContext.classLoader
                .getResourceAsStream('META-INF/services/io.micronaut.context.env.PropertySourceImporter')
                .readLines()

        expect:
        lines.contains('io.micronaut.kubernetes.configuration.imports.KubernetesPropertySourceImporter')
    }
}
