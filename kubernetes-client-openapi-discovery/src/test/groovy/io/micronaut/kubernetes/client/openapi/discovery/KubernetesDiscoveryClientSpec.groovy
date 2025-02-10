package io.micronaut.kubernetes.client.openapi.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.K3sContainerSpec
import io.micronaut.kubernetes.client.openapi.model.V1Service
import io.micronaut.kubernetes.client.openapi.model.V1ServiceSpec
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import io.micronaut.kubernetes.client.openapi.utils.ModelUtils
import io.micronaut.kubernetes.client.openapi.utils.OperationUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import spock.lang.Unroll

class KubernetesDiscoveryClientSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesDiscoveryClientSpec.class)

    private static final NAMESPACE_NAME = "micronaut-discovery"

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(CoreV1ApiReactor api) {
        OperationUtils.createNamespace(api, ModelUtils.getNamespace(NAMESPACE_NAME))
        V1ServiceSpec spec = ModelUtils.getServiceSpec("test-external.com")
        V1Service service = ModelUtils.getService("test-service", spec)
        OperationUtils.createService(api, NAMESPACE_NAME, service)
    }

    @Unroll
    void "get instances using service discovery mode [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME,
                "kubernetes.client.discovery.mode"                                     : "service",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        def discoveryClient = context.getBean(KubernetesDiscoveryClient)

        when:
        def serviceInstances = Mono.from(discoveryClient.getInstances("test-service")).block()

        then:
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "test-service"
        serviceInstances.get(0).URI.toString() == "http://test-external.com"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service ids using service discovery mode [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME,
                "kubernetes.client.discovery.mode"                                     : "service",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        def discoveryClient = context.getBean(KubernetesDiscoveryClient)

        when:
        def serviceIds = Mono.from(discoveryClient.getServiceIds()).block()

        then:
        serviceIds.size() == 1
        serviceIds.contains("test-service")

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }
}
