package io.micronaut.kubernetes.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Specification

/**
 *
 *
 * @author Pavol Gressa
 * @since 2.3
 */
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
}
