package io.micronaut.kubernetes.discovery.provider

import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServicePortBuilder
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.v1.KubernetesServiceConfiguration
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Requires
import spock.lang.Shared

import jakarta.inject.Inject

@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "micronaut-service-provider")
@Property(name = "spec.reuseNamespace", value = "false")
class KubernetesServiceInstanceServiceProviderSpec extends KubernetesSpecification {

    @Inject
    @Shared
    KubernetesServiceInstanceServiceProvider provider

    void "it returns nothing when service doesn't exists"(){
        when:
        def config = createConfig("a-service")
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 0
    }

    void "it can't get headless service ip when using mode service"(){
        given:
        operations.createService("headless-service", namespace,
                new ServiceSpecBuilder()
                        .withClusterIP("None")
                        .withPorts(
                                new ServicePortBuilder()
                                        .withPort(8081)
                                        .withTargetPort(new IntOrString(8081))
                                        .build()
                        )
                        .withSelector(["app": "example-service"])
                        .build())

        when:
        def config = createConfig("headless-service")
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 0
    }

    void "it gets port ip for multi port service"(){
        given:
        operations.createService("multiport-service", namespace,
                new ServiceSpecBuilder()
                        .withPorts(
                                new ServicePortBuilder()
                                        .withName("jvm-debug")
                                        .withPort(5004)
                                        .withTargetPort(new IntOrString("jvm-debug"))
                                        .build(),
                                new ServicePortBuilder()
                                        .withName("http")
                                        .withPort(8081)
                                        .withTargetPort(new IntOrString("http"))
                                        .build()
                        )
                        .withSelector(["app": "example-service"])
                        .build())

        when: 'no port is specified'
        def config = createConfig("multiport-service")

        then: 'the returned list is empty'
        Flowable.fromPublisher(provider.getInstances(config)).blockingFirst().isEmpty()

        when: 'http port is specified'
        config.port = 'http'
        def instances = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then: 'two service instances with port 8081 are discovered'
        instances.size() == 1
        instances.first().port == 8081
        operations.getService("multiport-service", namespace).spec.clusterIP == instances.first().host
    }

    void "it can get service from other then app namespace"(){
        given:
        createNamespaceSafe("other-namespace")
        createBaseResources("other-namespace")
        createExampleServiceDeployment("other-namespace")

        def config = createConfig("example-service")
        config.namespace = "other-namespace"

        when:
        def instances = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instances.size() == 1
        operations.getService("example-service", "other-namespace")
                .spec.clusterIP == instances.first().host

        cleanup:
        operations.deleteNamespace("other-namespace")
    }

    void "it can get external https service when using mode service"(){
        given:
        Service service = operations.createService("external-service-https", namespace,
                new ServiceSpecBuilder()
                        .withType("ExternalName")
                        .withExternalName("launch.micronaut.io")
                        .withPorts(new ServicePortBuilder()
                                .withPort(443)
                                .withTargetPort(new IntOrString(443))
                                .build())
                        .build())

        when:
        def config = createConfig("external-service-https")
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 1
        with(instanceList.first().getURI().toString()){
            it.startsWith("https://")
            it.endsWith(":443")
        }

        cleanup:
        operations.deleteService(service)
    }

    void "it can get external http service when using mode service"(){
        given:
        Service service = operations.createService("external-service-http", namespace,
                new ServiceSpecBuilder()
                        .withType("ExternalName")
                        .withExternalName("launch.micronaut.io")
                        .build())

        when:
        def config = createConfig("external-service-http")
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 1
        with(instanceList.first().getURI().toString()){
            it.startsWith("http://")
            it.endsWith(":80")
        }

        cleanup:
        operations.deleteService(service)
    }

    void "it ignores includes filter for manually configured service"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.discovery.includes": "example-service"], Environment.KUBERNETES)
        KubernetesServiceInstanceServiceProvider provider = applicationContext.getBean(KubernetesServiceInstanceServiceProvider)

        when:
        def config = createConfig("example-client", true)
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 1

        cleanup:
        applicationContext.close()
    }

    void "it ignores excludes filter for manually configured service"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.discovery.excludes": "example-service"], Environment.KUBERNETES)
        KubernetesServiceInstanceServiceProvider provider = applicationContext.getBean(KubernetesServiceInstanceServiceProvider)

        when:
        def config = createConfig("example-service", true)
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 1

        cleanup:
        applicationContext.close()
    }

    void "it ignores label filter for manually configured service"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.discovery.labels": [foo:"bar"]], Environment.KUBERNETES)
        KubernetesServiceInstanceServiceProvider provider = applicationContext.getBean(KubernetesServiceInstanceServiceProvider)

        when:
        def config = createConfig("example-client", true)
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 1

        when:
        config = createConfig("example-service", true)
        instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 1

        cleanup:
        applicationContext.close()
    }

    KubernetesServiceConfiguration createConfig(String name, manual = false){
        return new KubernetesServiceConfiguration(
                name,
                name,
                namespace,
                KubernetesServiceInstanceServiceProvider.MODE,
                null,
                manual)
    }
}
