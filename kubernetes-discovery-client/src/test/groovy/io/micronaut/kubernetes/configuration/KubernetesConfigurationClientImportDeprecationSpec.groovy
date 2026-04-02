package io.micronaut.kubernetes.configuration

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.kubernetes.KubernetesConfiguration
import io.micronaut.kubernetes.client.reactor.CoreV1ApiReactorClient
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import spock.lang.Specification

class KubernetesConfigurationClientImportDeprecationSpec extends Specification {

    void cleanup() {
        KubernetesConfigurationClient.getPropertySourceCache().clear()
    }

    void "explicit config map import makes classic config map bootstrap back off cleanly"() {
        given:
        def legacyImportMode = new KubernetesLegacyImportMode()
        legacyImportMode.registerExplicitImport(KubernetesLegacyImportMode.LegacyType.CONFIG_MAP)
        def client = configurationClient(configuration(true, false), legacyImportMode)

        when:
        def propertySources = Flux.from(client.getPropertySources(environment())).collectList().block()

        then:
        propertySources.empty
    }

    void "explicit secret import makes classic secret bootstrap back off cleanly"() {
        given:
        def legacyImportMode = new KubernetesLegacyImportMode()
        legacyImportMode.registerExplicitImport(KubernetesLegacyImportMode.LegacyType.SECRET)
        def client = configurationClient(configuration(false, true), legacyImportMode)

        when:
        def propertySources = Flux.from(client.getPropertySources(environment())).collectList().block()

        then:
        propertySources.empty
    }

    void "legacy bootstrap deprecation warning is logged once across repeated calls"() {
        given:
        def logger = (Logger) LoggerFactory.getLogger(KubernetesLegacyImportMode)
        def appender = new ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)
        logger.setLevel(Level.WARN)
        def legacyImportMode = new KubernetesLegacyImportMode()
        def client = configurationClient(configuration(true, true), legacyImportMode)

        when:
        Flux.from(client.getPropertySources(environment())).collectList().block()
        KubernetesConfigurationClient.getPropertySourceCache().clear()
        Flux.from(client.getPropertySources(environment())).collectList().block()

        then:
        appender.list.findAll { it.level == Level.WARN }.size() == 1
        appender.list.first().formattedMessage.contains('Legacy Kubernetes bootstrap configuration loading is deprecated')

        cleanup:
        logger.detachAppender(appender)
        appender.stop()
    }

    private KubernetesConfigurationClient configurationClient(KubernetesConfiguration configuration,
                                                              KubernetesLegacyImportMode legacyImportMode) {
        new KubernetesConfigurationClient(Stub(CoreV1ApiReactorClient), configuration, environment(), legacyImportMode)
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
