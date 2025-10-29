package io.micronaut.kubernetes.discovery.provider


import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.discovery.KubernetesServiceConfiguration
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import static io.micronaut.kubernetes.test.KubernetesModels.getServicePortModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServiceSpecClusterIPModel
import static io.micronaut.kubernetes.test.KubernetesModels.getServiceSpecTypeModel
import static io.micronaut.kubernetes.test.KubernetesOperations.createService
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteNamespace
import static io.micronaut.kubernetes.test.KubernetesOperations.deleteService
import static io.micronaut.kubernetes.test.KubernetesOperations.getService

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "micronaut-service-provider")
@Property(name = "spec.reuseNamespace", value = "false")
class KubernetesServiceInstanceServiceProviderSpec extends KubernetesSpecification {

    @Shared
    PollingConditions pollingConditions = new PollingConditions()

    @Unroll
    void "context contains #inContext [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                getConfig(watchEnabled),
                Environment.KUBERNETES)

        expect:
        applicationContext.containsBean(AbstractV1EndpointsProvider)
        applicationContext.containsBean(inContext)
        !applicationContext.containsBean(notInContext)

        cleanup:
        applicationContext.close()

        where:
        watchEnabled | inContext                                        | notInContext
        true         | KubernetesServiceInstanceServiceInformerProvider | KubernetesServiceInstanceServiceProvider
        false        | KubernetesServiceInstanceServiceProvider         | KubernetesServiceInstanceServiceInformerProvider
    }

    @Unroll
    void "it returns nothing when service doesn't exists [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                getConfig(watchEnabled),
                Environment.KUBERNETES)
        def provider = applicationContext.getBean(AbstractV1ServiceProvider)

        when:
        def config = createConfig("a-service")

        then:
        pollingConditions.eventually {
            Flux.from(provider.getInstances(config)).blockFirst().size() == 0
        }

        cleanup:
        applicationContext.close()

        where:
        watchEnabled << [true, false]
    }

    @Unroll
    void "it can't get headless service ip when using mode service [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                getConfig(watchEnabled),
                Environment.KUBERNETES)

        def provider = applicationContext.getBean(AbstractV1ServiceProvider)

        createService(
            "headless-service",
            namespace,
            getServiceSpecClusterIPModel("None", [getServicePortModel(8081, 8081)], ["app": "example-service"]))

        when:
        def config = createConfig("headless-service")

        then:
        pollingConditions.eventually {
            Flux.from(provider.getInstances(config)).blockFirst().size() == 0
        }

        cleanup:
        deleteService("headless-service", namespace)
        applicationContext.close()

        where:
        watchEnabled << [true, false]
    }

    @Unroll
    void "it gets port ip for multi port service [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                getConfig(watchEnabled),
                Environment.KUBERNETES)

        def provider = applicationContext.getBean(AbstractV1ServiceProvider)

        createService(
            "multiport-service",
            namespace,
            getServiceSpecTypeModel(
                "ClusterIP",
                [getServicePortModel("jvm-debug", 5004, "jvm-debug"), getServicePortModel("http", 8081, "http")],
                ["app": "example-service"]))

        when: 'no port is specified'
        def config = createConfig("multiport-service")

        then: 'the returned list is empty'
        pollingConditions.eventually {
            Flux.from(provider.getInstances(config)).blockFirst().isEmpty()
        }

        when: 'http port is specified'
        config.port = 'http'

        then: 'two service instances with port 8081 are discovered'
        pollingConditions.eventually {
            def instances = Flux.from(provider.getInstances(config)).blockFirst()
            instances.size() == 1
            instances.first().port == 8081
            getService("multiport-service", namespace).spec.clusterIP == instances.first().host
        }

        cleanup:
        deleteService("multiport-service", namespace)
        applicationContext.close()

        where:
        watchEnabled << [true, false]
    }

    @Unroll
    void "it can get service from other then app namespace [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                getConfig(watchEnabled, ["kubernetes.client.discovery.services.example-service": ["namespace": "other-namespace-2"]]),
                Environment.KUBERNETES)
        def provider = applicationContext.getBean(AbstractV1ServiceProvider)

        createNamespaceSafe("other-namespace-2")
        createBaseResources("other-namespace-2")
        createExampleServiceDeployment("other-namespace-2")

        when:
        def config = createConfig("example-service")
        config.namespace = "other-namespace-2"

        then:
        pollingConditions.eventually {
            def instances = Flux.from(provider.getInstances(config)).blockFirst()
            instances.size() == 1
            getService("example-service", "other-namespace-2")
                    .spec.clusterIP == instances.first().host
        }

        cleanup:
        deleteNamespace("other-namespace-2")
        applicationContext.close()

        where:
        watchEnabled << [true, false]
    }

    @Unroll
    void "it can get external https service when using mode service [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                getConfig(watchEnabled),
                Environment.KUBERNETES)
        def provider = applicationContext.getBean(AbstractV1ServiceProvider)

        createService(
            "external-service-https",
            namespace,
            getServiceSpecTypeModel("ExternalName", [getServicePortModel(443, 443)], [:], "launch.micronaut.io"))

        when:
        def config = createConfig("external-service-https")

        then:
        pollingConditions.eventually {
            def instanceList = Flux.from(provider.getInstances(config)).blockFirst()
            instanceList.size() == 1
            with(instanceList.first().getURI().toString()) {
                it.startsWith("https://")
                it.endsWith(":443")
            }
        }

        cleanup:
        deleteService("external-service-https", namespace)
        applicationContext.close()

        where:
        watchEnabled << [true, false]
    }

    @Unroll
    void "it can get external http service when using mode service [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                getConfig(watchEnabled),
                Environment.KUBERNETES)

        def provider = applicationContext.getBean(AbstractV1ServiceProvider)

        createService(
            "external-service-http",
            namespace,
            getServiceSpecTypeModel("ExternalName", [], [:], "launch.micronaut.io"))

        when:
        def config = createConfig("external-service-http")

        then:
        pollingConditions.eventually {
            def instanceList = Flux.from(provider.getInstances(config)).blockFirst()
            instanceList.size() == 1
            with(instanceList.first().getURI().toString()) {
                it.startsWith("http://")
                it.endsWith(":80")
            }
        }

        cleanup:
        deleteService("external-service-http", namespace)
        applicationContext.close()

        where:
        watchEnabled << [true, false]
    }

    @Unroll
    void "it ignores includes filter for manually configured service [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                getConfig(watchEnabled, ["kubernetes.client.discovery.includes": "example-service"]),
                Environment.KUBERNETES)
        def provider = applicationContext.getBean(AbstractV1ServiceProvider)

        when:
        def config = createConfig("example-client", true)

        then:
        pollingConditions.eventually {
            Flux.from(provider.getInstances(config)).blockFirst().size() == 1
        }

        cleanup:
        applicationContext.close()

        where:
        watchEnabled << [true, false]
    }

    @Unroll
    void "it ignores excludes filter for manually configured service [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                getConfig(watchEnabled, ["kubernetes.client.discovery.excludes": "example-service"]),
                Environment.KUBERNETES)

        def provider = applicationContext.getBean(AbstractV1ServiceProvider)

        when:
        def config = createConfig("example-service", true)

        then:
        pollingConditions.eventually {
            Flux.from(provider.getInstances(config)).blockFirst().size() == 1
        }

        cleanup:
        applicationContext.close()

        where:
        watchEnabled << [true, false]
    }

    @Unroll
    void "it ignores label filter for manually configured service [watchEnabled=#watchEnabled]"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                getConfig(watchEnabled, ["kubernetes.client.discovery.labels": [foo: "bar"]]),
                Environment.KUBERNETES)
        def provider = applicationContext.getBean(AbstractV1ServiceProvider)

        when:
        def config = createConfig("example-client", true)

        then:
        pollingConditions.eventually {
            Flux.from(provider.getInstances(config)).blockFirst().size() == 1
        }

        when:
        config = createConfig("example-service", true)

        then:
        pollingConditions.eventually {
            Flux.from(provider.getInstances(config)).blockFirst().size() == 1
        }

        cleanup:
        applicationContext.close()

        where:
        watchEnabled << [true, false]
    }

    KubernetesServiceConfiguration createConfig(String name, manual = false) {
        return new KubernetesServiceConfiguration(
                name,
                name,
                namespace,
                KubernetesServiceInstanceServiceProvider.MODE,
                null,
                manual)
    }

    Map<String, Object> getConfig(boolean watchEnabled, Map<String, Object> additional = [:]) {
        Map<String, Object> config = [
                "kubernetes.client.namespace"                                         : namespace,
                "kubernetes.client.discovery.mode"                                    : "service",
                "kubernetes.client.discovery.mode-configuration.service.watch.enabled": watchEnabled
        ]
        config.putAll(additional)
        return config
    }
}
