package io.micronaut.kubernetes.client.openapi.discovery

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1Service
import io.micronaut.kubernetes.client.openapi.model.V1ServiceSpec
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import spock.lang.Unroll

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getNamespaceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getServiceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getServiceSpecTypeModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createService

class KubernetesDiscoveryClientSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesDiscoveryClientSpec.class)

    private static final NAMESPACE_NAME = "micronaut-discovery"

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(ApplicationContext context) {
        CoreV1Api api = context.getBean(CoreV1Api.class)
        createNamespace(api, getNamespaceModel(NAMESPACE_NAME))
        V1ServiceSpec spec = getServiceSpecTypeModel("ExternalName", [], [:], "test-external.com")
        V1Service service = getServiceModel("test-service", spec)
        createService(api, NAMESPACE_NAME, service)
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
