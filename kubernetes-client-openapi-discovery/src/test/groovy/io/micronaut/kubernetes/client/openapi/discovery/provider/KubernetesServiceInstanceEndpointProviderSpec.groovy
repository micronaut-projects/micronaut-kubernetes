package io.micronaut.kubernetes.client.openapi.discovery.provider

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.discovery.KubernetesServiceConfiguration
import io.micronaut.kubernetes.client.openapi.model.V1EndpointSubset
import io.micronaut.kubernetes.client.openapi.model.V1Endpoints
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import io.micronaut.kubernetes.client.openapi.utils.ModelUtils
import io.micronaut.kubernetes.client.openapi.utils.OperationUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

class KubernetesServiceInstanceEndpointProviderSpec extends Specification {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceEndpointProviderSpec.class)
    private static final Logger LOG_K3S = LoggerFactory.getLogger("K3S." + KubernetesServiceInstanceEndpointProviderSpec.getSimpleName())

    private static final NAMESPACE_NAME_1 = "micronaut-endpoint-provider-1"
    private static final NAMESPACE_NAME_2 = "micronaut-endpoint-provider-2"

    @Shared
    @AutoCleanup
    K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
            .withLogConsumer(new Slf4jLogConsumer(LOG_K3S))

    @Shared
    Path kubeConfigDir = Files.createTempDirectory("kube-temp-")

    @Shared
    Path kubeConfigFile = kubeConfigDir.resolve("config")

    def setupSpec() {
        k3s.start()
        kubeConfigFile.toFile().text = k3s.getKubeConfigYaml()
        LOG.info("Kubernetes config file path: {}", kubeConfigFile)
        setupKubernetes()
    }

    def setupKubernetes() {
        try (ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"   : false,
                "kubernetes.client.kube-config-path": "file:" + kubeConfigFile.toString(),
        ])) {
            def api = context.getBean(CoreV1ApiReactor.class)

            OperationUtils.createNamespace(api, ModelUtils.getNamespace(NAMESPACE_NAME_1))
            OperationUtils.createNamespace(api, ModelUtils.getNamespace(NAMESPACE_NAME_2))

            // invalid endpoint - no subsets
            V1Endpoints endpoints1 = ModelUtils.getEndpoints("endpoints-example-1", [])
            OperationUtils.createEndpoints(api, NAMESPACE_NAME_1, endpoints1)

            // invalid endpoint - no ports
            V1EndpointSubset endpointSubset2 = ModelUtils.getEndpointSubset([ModelUtils.getEndpointAddress("10.244.0.5")], [])
            V1Endpoints endpoints2 = ModelUtils.getEndpoints("endpoints-example-2", [endpointSubset2])
            OperationUtils.createEndpoints(api, NAMESPACE_NAME_1, endpoints2)

            // single endpoint with one address and two ports
            V1EndpointSubset endpointSubset3 = ModelUtils.getEndpointSubset(
                    [ModelUtils.getEndpointAddress("0.0.0.1")],
                    [ModelUtils.getEndpointPort(8081, "http"), ModelUtils.getEndpointPort(8082, "https")])
            V1Endpoints endpoints3 = ModelUtils.getEndpoints("endpoints-example-3", [endpointSubset3])
            OperationUtils.createEndpoints(api, NAMESPACE_NAME_1, endpoints3)

            // single endpoint subset with two addresses and one port
            V1EndpointSubset endpointSubset4 = ModelUtils.getEndpointSubset(
                    [ModelUtils.getEndpointAddress("1.0.0.1"), ModelUtils.getEndpointAddress("1.0.0.2")],
                    [ModelUtils.getEndpointPort(8443)])
            V1Endpoints endpoints4 = ModelUtils.getEndpoints("endpoints-example-4", [endpointSubset4], ["foo": "bar"])
            OperationUtils.createEndpoints(api, NAMESPACE_NAME_1, endpoints4)

            // multiple endpoint subsets
            V1EndpointSubset endpointSubset51 = ModelUtils.getEndpointSubset(
                    [ModelUtils.getEndpointAddress("2.0.0.1"), ModelUtils.getEndpointAddress("2.0.0.2")],
                    [ModelUtils.getEndpointPort(8081)])
            V1EndpointSubset endpointSubset52 = ModelUtils.getEndpointSubset(
                    [ModelUtils.getEndpointAddress("3.0.0.1")],
                    [ModelUtils.getEndpointPort(8082)])
            V1Endpoints endpoints5 = ModelUtils.getEndpoints("endpoints-example-5", [endpointSubset51, endpointSubset52])
            OperationUtils.createEndpoints(api, NAMESPACE_NAME_1, endpoints5)

            // endpoint in another namespace
            V1EndpointSubset endpointSubset6 = ModelUtils.getEndpointSubset(
                    [ModelUtils.getEndpointAddress("100.0.0.1")],
                    [ModelUtils.getEndpointPort(8080, "http")])
            V1Endpoints endpoints6 = ModelUtils.getEndpoints("endpoints-example-6", [endpointSubset6])
            OperationUtils.createEndpoints(api, NAMESPACE_NAME_2, endpoints6)
        }
    }

    def cleanupSpec() {
        if (kubeConfigFile != null) {
            Files.deleteIfExists(kubeConfigFile)
        }
        if (kubeConfigDir) {
            Files.deleteIfExists(kubeConfigDir)
        }
    }

    @Unroll
    void "context contains #inContext [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        expect:
        context.containsBean(AbstractV1EndpointsProvider)
        context.containsBean(inContext)
        !context.containsBean(notInContext)

        cleanup:
        context.close()

        where:
        watchEnabled | inContext                                         | notInContext
        true         | KubernetesServiceInstanceEndpointInformerProvider | KubernetesServiceInstanceEndpointProvider
        false        | KubernetesServiceInstanceEndpointProvider         | KubernetesServiceInstanceEndpointInformerProvider
    }

    @Unroll
    void "test validation [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when: "endpoints object doesn't contain any endpoint subset objects"
        def serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-1")
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "validation fails and there are no returned service instances"
        serviceInstances.size() == 0

        when: "endpoint subset object doesn't contain any port objects"
        serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-2")
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "validation fails and there are no returned service instances"
        serviceInstances.size() == 0

        when: "endpoint subset object has two ports and service configuration doesn't provide info which one should be used"
        serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-3")
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "validation fails and there are no returned service instances"
        serviceInstances.size() == 0

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service instances using endpoint mode and [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when: "port name is found in service configuration"
        def serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-3", "http")
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "service instance url should contain port from service configuration"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "endpoints-example-3"
        serviceInstances.get(0).URI.toString() == "http://0.0.0.1:8081"

        when: "port name from service configuration is equal to https"
        serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-3", "https")
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "service instance url should contain https scheme"
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "endpoints-example-3"
        serviceInstances.get(0).URI.toString() == "https://0.0.0.1:8082"

        when: "port number ends with 443"
        serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-4")
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "service instance url should contain https scheme"
        serviceInstances.size() == 2
        serviceInstances.get(0).id == "endpoints-example-4"
        serviceInstances.get(0).URI.toString() == "https://1.0.0.1:8443"
        serviceInstances.get(1).id == "endpoints-example-4"
        serviceInstances.get(1).URI.toString() == "https://1.0.0.2:8443"

        when: "multiple endpoint subset are found"
        serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-5")
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "service instances should be created from all endpoint subnets"
        serviceInstances.size() == 3
        serviceInstances.get(0).id == "endpoints-example-5"
        serviceInstances.get(0).URI.toString() == "http://2.0.0.1:8081"
        serviceInstances.get(1).id == "endpoints-example-5"
        serviceInstances.get(1).URI.toString() == "http://2.0.0.2:8081"
        serviceInstances.get(2).id == "endpoints-example-5"
        serviceInstances.get(2).URI.toString() == "http://3.0.0.1:8082"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service instances from other then app namespace [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.services.endpoints-example-6.namespace"   : NAMESPACE_NAME_2
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when:
        def serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-6", null, NAMESPACE_NAME_2)
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then:
        serviceInstances.size() == 1
        serviceInstances.get(0).id == "endpoints-example-6"
        serviceInstances.get(0).URI.toString() == "http://100.0.0.1:8080"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service instances when exclude filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.excludes"                                 : "endpoints-example-4"
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when: "service configured automatically"
        def serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-4", false)
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "exclude filter applied so there are no returned service instances"
        serviceInstances.size() == 0

        when: "service configured manually"
        serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-4", true)
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "exclude filter not applied"
        serviceInstances.size() == 2
        serviceInstances.get(0).id == "endpoints-example-4"
        serviceInstances.get(0).URI.toString() == "https://1.0.0.1:8443"
        serviceInstances.get(1).id == "endpoints-example-4"
        serviceInstances.get(1).URI.toString() == "https://1.0.0.2:8443"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service instances when include filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.includes"                                 : "aaaaa"
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when: "service configured automatically"
        def serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-4", false)
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "include filter applied so there are no returned service instances"
        serviceInstances.size() == 0

        when: "service configured manually"
        serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-4", true)
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "include filter not applied"
        serviceInstances.size() == 2
        serviceInstances.get(0).id == "endpoints-example-4"
        serviceInstances.get(0).URI.toString() == "https://1.0.0.1:8443"
        serviceInstances.get(1).id == "endpoints-example-4"
        serviceInstances.get(1).URI.toString() == "https://1.0.0.2:8443"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service instances when label filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.labels"                                   : [foo: "bar"]
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when: "service configured automatically for endpoints-example-5"
        def serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-5", false)
        def serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "label filter applied so there are no returned service instances"
        serviceInstances.size() == 0

        when: "service configured manually for endpoints-example-5"
        serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-5", true)
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "label filter not applied"
        serviceInstances.size() == 3
        serviceInstances.get(0).id == "endpoints-example-5"
        serviceInstances.get(0).URI.toString() == "http://2.0.0.1:8081"
        serviceInstances.get(1).id == "endpoints-example-5"
        serviceInstances.get(1).URI.toString() == "http://2.0.0.2:8081"
        serviceInstances.get(2).id == "endpoints-example-5"
        serviceInstances.get(2).URI.toString() == "http://3.0.0.1:8082"

        when: "service configured automatically for endpoints-example-4"
        serviceConfiguration = createKubernetesServiceConfiguration("endpoints-example-4", false)
        serviceInstances = Mono.from(provider.getInstances(serviceConfiguration)).block()

        then: "label filter applied and matched endpoints-example-4"
        serviceInstances.size() == 2
        serviceInstances.get(0).id == "endpoints-example-4"
        serviceInstances.get(0).URI.toString() == "https://1.0.0.1:8443"
        serviceInstances.get(1).id == "endpoints-example-4"
        serviceInstances.get(1).URI.toString() == "https://1.0.0.2:8443"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service ids [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when:
        def serviceIds = Flux.from(provider.getServiceIds(NAMESPACE_NAME_1)).collectList().block()

        then:
        serviceIds.size() == 5
        serviceIds.contains("endpoints-example-1")
        serviceIds.contains("endpoints-example-2")
        serviceIds.contains("endpoints-example-3")
        serviceIds.contains("endpoints-example-4")
        serviceIds.contains("endpoints-example-5")

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service ids from other then app namespace [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.services.endpoints-example-6.namespace"   : NAMESPACE_NAME_2
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when:
        def serviceIds = Flux.from(provider.getServiceIds(NAMESPACE_NAME_2)).collectList().block()

        then:
        serviceIds.size() == 1
        serviceIds.contains("endpoints-example-6")

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service ids when exclude filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.excludes"                                 : ["endpoints-example-1", "endpoints-example-2", "endpoints-example-3"]
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when:
        def serviceIds = Flux.from(provider.getServiceIds(NAMESPACE_NAME_1)).collectList().block()

        then:
        serviceIds.size() == 2
        serviceIds.contains("endpoints-example-4")
        serviceIds.contains("endpoints-example-5")

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service ids when include filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.includes"                                 : ["endpoints-example-3", "endpoints-example-5"]
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when:
        def serviceIds = Flux.from(provider.getServiceIds(NAMESPACE_NAME_1)).collectList().block()

        then:
        serviceIds.size() == 2
        serviceIds.contains("endpoints-example-3")
        serviceIds.contains("endpoints-example-5")

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    @Unroll
    void "get service ids when label filter is set [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.config-client.enabled"                                      : false,
                "kubernetes.client.kube-config-path"                                   : "file:" + kubeConfigFile.toString(),
                "kubernetes.client.namespace"                                          : NAMESPACE_NAME_1,
                "kubernetes.client.discovery.mode"                                     : "endpoint",
                "kubernetes.client.discovery.mode-configuration.endpoint.watch.enabled": watchEnabled,
                "kubernetes.client.discovery.labels"                                   : [foo: "bar"]
        ], Environment.KUBERNETES)

        def provider = context.getBean(AbstractV1EndpointsProvider)

        when:
        def serviceIds = Flux.from(provider.getServiceIds(NAMESPACE_NAME_1)).collectList().block()

        then:
        serviceIds.size() == 1
        serviceIds.get(0) == "endpoints-example-4"

        cleanup:
        context.close()

        where:
        watchEnabled << [false, true]
    }

    KubernetesServiceConfiguration createKubernetesServiceConfiguration(String endpointName) {
        createKubernetesServiceConfiguration(endpointName, null)
    }

    KubernetesServiceConfiguration createKubernetesServiceConfiguration(String endpointName, String portName) {
        createKubernetesServiceConfiguration(endpointName, portName, NAMESPACE_NAME_1)
    }

    KubernetesServiceConfiguration createKubernetesServiceConfiguration(String endpointName, String portName, String namespace) {
        createKubernetesServiceConfiguration(endpointName, portName, namespace, false)
    }

    KubernetesServiceConfiguration createKubernetesServiceConfiguration(String endpointName, boolean manual) {
        createKubernetesServiceConfiguration(endpointName, null, NAMESPACE_NAME_1, manual)
    }

    KubernetesServiceConfiguration createKubernetesServiceConfiguration(String endpointName, String portName, String namespace, boolean manual) {
        new KubernetesServiceConfiguration(endpointName, endpointName, namespace, "endpoint", portName, manual)
    }

}
