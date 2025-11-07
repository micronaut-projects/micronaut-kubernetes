package io.micronaut.kubernetes.client.openapi.discovery.provider

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.discovery.KubernetesServiceConfiguration
import io.micronaut.kubernetes.client.openapi.model.V1Service
import io.micronaut.kubernetes.client.openapi.model.V1ServiceSpec
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Unroll

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getNamespaceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getServiceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getServicePortModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getServiceSpecClusterIPModel
import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getServiceSpecTypeModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createNamespace
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createService

class KubernetesServiceInstanceServiceProviderSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceServiceProviderSpec.class)

    private static final NAMESPACE_NAME_1 = "micronaut-service-provider-1"
    private static final NAMESPACE_NAME_2 = "micronaut-service-provider-2"

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(ApplicationContext context) {
        CoreV1Api api = context.getBean(CoreV1Api.class)
        createNamespace(api, getNamespaceModel(NAMESPACE_NAME_1))
        createNamespace(api, getNamespaceModel(NAMESPACE_NAME_2))

        // service with two ports
        V1ServiceSpec spec1 = getServiceSpecClusterIPModel("10.43.1.100", [getServicePortModel(8081, null, "http"), getServicePortModel(8082, null, "https")])
        V1Service service1 = getServiceModel("service-example-1", spec1)
        createService(api, NAMESPACE_NAME_1, service1)

        // service with single port
        V1ServiceSpec spec2 = getServiceSpecClusterIPModel("10.43.1.101", [getServicePortModel(8443, null, "port")])
        V1Service service2 = getServiceModel("service-example-2", spec2, ["foo": "bar"])
        createService(api, NAMESPACE_NAME_1, service2)

        // service with external name
        V1ServiceSpec spec3 = getServiceSpecTypeModel("ExternalName", [], [:], "test-external.com")
        V1Service service3 = getServiceModel("service-example-3", spec3)
        createService(api, NAMESPACE_NAME_1, service3)

        // service with external name and secure label
        V1ServiceSpec spec4 = getServiceSpecTypeModel("ExternalName", [], [:], "test-external-secure.com")
        V1Service service4 = getServiceModel("service-example-4", spec4, ["secure": "true"])
        createService(api, NAMESPACE_NAME_1, service4)

        // service in another namespace
        V1ServiceSpec spec5 = getServiceSpecClusterIPModel("10.43.1.201", [getServicePortModel(8080, null, "port")])
        V1Service service5 = getServiceModel("service-example-5", spec5)
        createService(api, NAMESPACE_NAME_2, service5)
    }

    @Unroll
    void "context contains #inContext [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        expect:
        context.containsBean(AbstractV1ServiceProvider)
        context.containsBean(inContext)
        !context.containsBean(notInContext)

        cleanup:
        context.close()

        where:
        watchEnabled | inContext                                        | notInContext
        true         | KubernetesServiceInstanceServiceInformerProvider | KubernetesServiceInstanceServiceProvider
        false        | KubernetesServiceInstanceServiceProvider         | KubernetesServiceInstanceServiceInformerProvider
    }

    @Unroll
    void "test validation [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when: "service spec object has two ports and service configuration doesn't provide info which one should be used"
        def serviceConfiguration = createKubernetesServiceConfiguration("service-example-1")
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "validation fails and there are no returned service instances"
        serviceInstances.size() == 0

        when: "service spec object has two ports and service configuration provides port name which doesn't match any port"
        serviceConfiguration = createKubernetesServiceConfiguration("service-example-1", "invalid-port-name")
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "validation fails and there are no returned service instances"
        serviceInstances.size() == 0

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service instances [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when: "port name is found in service configuration"
        def serviceConfiguration = createKubernetesServiceConfiguration("service-example-1", "http")
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "service instance url should contain port from service configuration"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "service-example-1"
        serviceInstances.get(0).URI.toString() == "http://10.43.1.100:8081"

        when: "port name from service configuration is equal to https"
        serviceConfiguration = createKubernetesServiceConfiguration("service-example-1", "https")
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "service instance url should contain https scheme"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "service-example-1"
        serviceInstances.get(0).URI.toString() == "https://10.43.1.100:8082"

        when: "port number ends with 443"
        serviceConfiguration = createKubernetesServiceConfiguration("service-example-2")
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "service instance url should contain https scheme"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "service-example-2"
        serviceInstances.get(0).URI.toString() == "https://10.43.1.101:8443"

        when: "service spec type is set to ExternalName"
        serviceConfiguration = createKubernetesServiceConfiguration("service-example-3")
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "service instance url should contain domain from service spec externalName field"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "service-example-3"
        serviceInstances.get(0).URI.toString() == "http://test-external.com"

        when: "service spec type is set to ExternalName and secure label used"
        serviceConfiguration = createKubernetesServiceConfiguration("service-example-4")
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "service instance url should contain https scheme and domain from service spec externalName field"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "service-example-4"
        serviceInstances.get(0).URI.toString() == "https://test-external-secure.com"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service instances from other then app namespace [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.services.service-example-5.namespace"    : NAMESPACE_NAME_2
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when:
        def serviceConfiguration = createKubernetesServiceConfiguration("service-example-5", null, NAMESPACE_NAME_2)
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then:
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "service-example-5"
        serviceInstances.get(0).URI.toString() == "http://10.43.1.201:8080"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service instances when exclude filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.excludes"                                : "service-example-2"
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when: "service configured automatically"
        def serviceConfiguration = createKubernetesServiceConfiguration("service-example-2", false)
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "exclude filter applied so there are no returned service instances"
        serviceInstances.size() == 0

        when: "service configured manually"
        serviceConfiguration = createKubernetesServiceConfiguration("service-example-2", true)
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "exclude filter not applied"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "service-example-2"
        serviceInstances.get(0).URI.toString() == "https://10.43.1.101:8443"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service instances when include filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.includes"                                : "aaaaa"
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when: "service configured automatically"
        def serviceConfiguration = createKubernetesServiceConfiguration("service-example-2", false)
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "include filter applied so there are no returned service instances"
        serviceInstances.size() == 0

        when: "service configured manually"
        serviceConfiguration = createKubernetesServiceConfiguration("service-example-2", true)
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "include filter not applied"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "service-example-2"
        serviceInstances.get(0).URI.toString() == "https://10.43.1.101:8443"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service instances when label filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.labels"                                  : [foo: "bar"]
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when: "service configured automatically for service-example-3"
        def serviceConfiguration = createKubernetesServiceConfiguration("service-example-3", false)
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "label filter applied and not matched service-example-3"
        serviceInstances.size() == 0

        when: "service configured manually for service-example-3"
        serviceConfiguration = createKubernetesServiceConfiguration("service-example-3", true)
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "label filter not applied"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "service-example-3"
        serviceInstances.get(0).URI.toString() == "http://test-external.com"

        when: "service configured automatically for service-example-2"
        serviceConfiguration = createKubernetesServiceConfiguration("service-example-2", false)
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "label filter applied and matched service-example-2"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "service-example-2"
        serviceInstances.get(0).URI.toString() == "https://10.43.1.101:8443"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service ids [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when:
        def serviceIds = Flux.from(provider.getServiceIds(NAMESPACE_NAME_1)).collectList().block()

        then:
        serviceIds.size() == 4
        serviceIds.contains("service-example-1")
        serviceIds.contains("service-example-2")
        serviceIds.contains("service-example-3")
        serviceIds.contains("service-example-4")

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    void "get service ids from other then app namespace [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.services.endpoints-example-6.namespace"  : NAMESPACE_NAME_2
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when:
        def serviceIds = Flux.from(provider.getServiceIds(NAMESPACE_NAME_2)).collectList().block()

        then:
        serviceIds.size() == 1
        serviceIds.contains("service-example-5")

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service ids when exclude filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.excludes"                                : ["service-example-1", "service-example-3"]
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when:
        def serviceIds = Flux.from(provider.getServiceIds(NAMESPACE_NAME_1)).collectList().block()

        then:
        serviceIds.size() == 2
        serviceIds.contains("service-example-2")
        serviceIds.contains("service-example-4")

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service ids when include filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.includes"                                : ["service-example-1", "service-example-3"]
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when:
        def serviceIds = Flux.from(provider.getServiceIds(NAMESPACE_NAME_1)).collectList().block()

        then:
        serviceIds.size() == 2
        serviceIds.contains("service-example-1")
        serviceIds.contains("service-example-3")

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service ids when label filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                     : false,
                "kubernetes.client.kube-config-path"                                  : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                         : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.labels"                                  : [foo: "bar"]
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1ServiceProvider)

        when:
        def serviceIds = Flux.from(provider.getServiceIds(NAMESPACE_NAME_1)).collectList().block()

        then:
        serviceIds.size() == 1
        serviceIds.contains("service-example-2")

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    KubernetesServiceConfiguration createKubernetesServiceConfiguration(String serviceName) {
        createKubernetesServiceConfiguration(serviceName, null)
    }

    KubernetesServiceConfiguration createKubernetesServiceConfiguration(String serviceName, String portName) {
        createKubernetesServiceConfiguration(serviceName, portName, NAMESPACE_NAME_1)
    }

    KubernetesServiceConfiguration createKubernetesServiceConfiguration(String serviceName, String portName, String namespace) {
        createKubernetesServiceConfiguration(serviceName, portName, namespace, false)
    }

    KubernetesServiceConfiguration createKubernetesServiceConfiguration(String serviceName, boolean manual) {
        createKubernetesServiceConfiguration(serviceName, null, NAMESPACE_NAME_1, manual)
    }

    KubernetesServiceConfiguration createKubernetesServiceConfiguration(String serviceName, String portName, String namespace, boolean manual) {
        def config = new KubernetesServiceConfiguration(serviceName, manual)
        config.setName(serviceName)
        config.setPort(portName)
        config.setNamespace(namespace)
        config.setMode("service")
        return config
    }
}
