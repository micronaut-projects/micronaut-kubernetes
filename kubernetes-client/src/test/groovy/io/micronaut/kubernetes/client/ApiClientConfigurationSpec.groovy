package io.micronaut.kubernetes.client

import io.kubernetes.client.openapi.ApiClient
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class ApiClientConfigurationSpec extends Specification {

    def "test it creates empty configuration"(){
        given:
        ApplicationContext applicationContext = ApplicationContext.run()

        expect:
        applicationContext.containsBean(ApiClientConfiguration.class)
        applicationContext.getBean(ApiClientConfiguration.class).getVerifySsl()
    }

    def "test it sets properties"(){
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.basePath": "basePath",
                "kubernetes.client.tokenPath": "tokenPath",
                "kubernetes.client.caPath": "caPath",
                "kubernetes.client.kubeConfigPath": "kubeConfigPath"
        ])

        when:
        def configuration = applicationContext.getBean(ApiClientConfiguration.class)

        then:
        configuration
        configuration.basePath.isPresent()
        configuration.basePath.get() == "basePath"
        configuration.tokenPath.isPresent()
        configuration.tokenPath.get() == "tokenPath"
        configuration.caPath.isPresent()
        configuration.caPath.get() == "caPath"
        configuration.kubeConfigPath.isPresent()
        configuration.kubeConfigPath.get() == "kubeConfigPath"
    }

    def "test it sets properties for discovery cache"(){
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.api-discovery.cache.refresh-interval": 20
        ])

        when:
        def configuration = applicationContext.getBean(ApiClientConfiguration.ApiDiscoveryCacheConfiguration)

        then:
        configuration
        configuration.refreshInterval == 20
    }

    def "test it sets default properties for discovery cache"(){
        given:
        ApplicationContext applicationContext = ApplicationContext.run()

        when:
        def configuration = applicationContext.getBean(ApiClientConfiguration.ApiDiscoveryCacheConfiguration)

        then:
        configuration
        configuration.refreshInterval == Long.valueOf(ApiClientConfiguration.ApiDiscoveryCacheConfiguration.DEFAULT_REFRESH_INTERVAL)
    }

    def "it configured the api client's okhttp client"(){
        given:
        ApplicationContext applicationContext = ApplicationContext.run()

        when:
        ApiClient apiClient = applicationContext.getBean(ApiClient)

        then:
        apiClient.getHttpClient().readTimeoutMillis() == 5345
    }
}
