package io.micronaut.kubernetes.client.openapi.configuration

import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import reactor.core.publisher.Flux
import spock.lang.Specification

class KubernetesConfigurationClientImportDeprecationSpec extends Specification {

    void cleanup() {
        KubernetesConfigurationClient.getPropertySourceCache().clear()
    }

    void "explicit config map import makes openapi config map bootstrap back off cleanly"() {
        given:
        def legacyImportMode = new KubernetesLegacyImportMode()
        legacyImportMode.registerExplicitImport(KubernetesLegacyImportMode.LegacyType.CONFIG_MAP)
        def client = configurationClient(configuration(true, false), legacyImportMode)

        when:
        def propertySources = Flux.from(client.getPropertySources(environment())).collectList().block()

        then:
        propertySources.empty
    }

    void "explicit secret import makes openapi secret bootstrap back off cleanly"() {
        given:
        def legacyImportMode = new KubernetesLegacyImportMode()
        legacyImportMode.registerExplicitImport(KubernetesLegacyImportMode.LegacyType.SECRET)
        def client = configurationClient(configuration(false, true), legacyImportMode)

        when:
        def propertySources = Flux.from(client.getPropertySources(environment())).collectList().block()

        then:
        propertySources.empty
    }

    void "legacy bootstrap mode remains enabled without explicit imports"() {
        given:
        def legacyImportMode = new KubernetesLegacyImportMode()
        def client = configurationClient(configuration(true, true), legacyImportMode)

        when:
        Flux.from(client.getPropertySources(environment())).collectList().block()
        KubernetesConfigurationClient.getPropertySourceCache().clear()
        Flux.from(client.getPropertySources(environment())).collectList().block()

        then:
        legacyImportMode.isLegacyBootstrapEnabled(KubernetesLegacyImportMode.LegacyType.CONFIG_MAP)
        legacyImportMode.isLegacyBootstrapEnabled(KubernetesLegacyImportMode.LegacyType.SECRET)
    }

    private KubernetesConfigurationClient configurationClient(KubernetesConfiguration configuration,
                                                              KubernetesLegacyImportMode legacyImportMode) {
        new KubernetesConfigurationClient(Stub(CoreV1ApiReactor), configuration, environment(), legacyImportMode)
    }

    private KubernetesConfiguration configuration(boolean configMapsEnabled, boolean secretsEnabled) {
        def configuration = Stub(KubernetesConfiguration) {
            getNamespace() >> 'default'
        }
        def configMaps = Stub(KubernetesConfiguration.KubernetesConfigMapsConfiguration) {
            isEnabled() >> configMapsEnabled
            getPaths() >> []
            isUseApi() >> false
            getIncludes() >> []
            getExcludes() >> []
            getLabels() >> [:]
            getPodLabels() >> []
            isExceptionOnPodLabelsMissing() >> false
            isTerminateStartupOnException() >> false
        }
        def secrets = Stub(KubernetesConfiguration.KubernetesSecretsConfiguration) {
            isEnabled() >> secretsEnabled
            getPaths() >> []
            isUseApi() >> false
            getIncludes() >> []
            getExcludes() >> []
            getLabels() >> [:]
            getPodLabels() >> []
            isExceptionOnPodLabelsMissing() >> false
            isTerminateStartupOnException() >> false
        }
        configuration.getConfigMaps() >> configMaps
        configuration.getSecrets() >> secrets
        configuration
    }

    private Environment environment() {
        Stub(Environment) {
            getPropertySourceLoaders() >> []
        }
    }
}
