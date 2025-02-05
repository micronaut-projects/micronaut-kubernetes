package io.micronaut.kubernetes.client.openapi.discovery.provider

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.discovery.KubernetesServiceConfiguration
import io.micronaut.kubernetes.client.openapi.model.V1Service
import io.micronaut.kubernetes.client.openapi.model.V1ServiceSpec
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor
import io.micronaut.kubernetes.client.openapi.utils.ModelUtils
import io.micronaut.kubernetes.client.openapi.utils.OperationUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Mono
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

class KubernetesServiceInstanceServiceProviderSpec extends Specification {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceServiceProviderSpec.class)
    private static final Logger LOG_K3S = LoggerFactory.getLogger("K3S." + KubernetesServiceInstanceServiceProviderSpec.getSimpleName())

    private static final NAMESPACE_NAME_1 = "micronaut-service-provider-1"
    private static final NAMESPACE_NAME_2 = "micronaut-service-provider-2"

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

            // service with two ports
            V1ServiceSpec spec1 = ModelUtils.getServiceSpec("10.43.1.100", [ModelUtils.getServicePort(8081, "http"), ModelUtils.getServicePort(8082, "https")])
            V1Service service1 = ModelUtils.getService("service-example-1", spec1)
            OperationUtils.createService(api, NAMESPACE_NAME_1, service1)

            // service with single port
            V1ServiceSpec spec2 = ModelUtils.getServiceSpec("10.43.1.101", [ModelUtils.getServicePort(8443, "http")])
            V1Service service2 = ModelUtils.getService("service-example-2", spec2)
            OperationUtils.createService(api, NAMESPACE_NAME_1, service2)
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
