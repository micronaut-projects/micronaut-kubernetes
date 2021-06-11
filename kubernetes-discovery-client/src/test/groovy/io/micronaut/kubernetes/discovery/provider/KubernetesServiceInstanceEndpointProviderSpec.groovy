package io.micronaut.kubernetes.discovery.provider

import io.fabric8.kubernetes.api.model.Endpoints
import io.fabric8.kubernetes.api.model.EndpointsBuilder
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.ServicePortBuilder
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.configuration.KubernetesServiceConfiguration
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Requires
import spock.lang.Shared

import jakarta.inject.Inject


@MicronautTest(environments = [Environment.KUBERNETES])
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "micronaut-endpoint-provider")
@Property(name = "spec.reuseNamespace", value = "false")
class KubernetesServiceInstanceEndpointProviderSpec extends KubernetesSpecification {

    @Inject
    @Shared
    KubernetesServiceInstanceEndpointProvider provider

    void "it returns nothing when service endpoints doesn't exists"(){
        when:
        def config = createConfig("a-service")
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 0
    }

    void "it can get headless service ip by using mode endpoint"(){
        given:
        operations.createService("example-headless-service", namespace,
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
        def config = createConfig("example-headless-service")
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 2
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
        instances.size() == 2
        instances.stream().allMatch( s -> s.port == 8081)
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
        instances.size() == 2
        operations.getEndpoints("example-service", "other-namespace").subsets.stream()
                .allMatch(e ->
                        e.addresses.stream().allMatch(address ->
                            instances.any {it.host == address.ip}
                        )
                )

        cleanup:
        operations.deleteNamespace("other-namespace")
    }

    void "it ignores includes filter for manually configured service"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.discovery.includes": "example-service"], Environment.KUBERNETES)
        KubernetesServiceInstanceEndpointProvider provider = applicationContext.getBean(KubernetesServiceInstanceEndpointProvider)

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
        KubernetesServiceInstanceEndpointProvider provider = applicationContext.getBean(KubernetesServiceInstanceEndpointProvider)

        when:
        def config = createConfig("example-service", true)
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 2

        cleanup:
        applicationContext.close()
    }

    void "it ignores label filter for manually configured service"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "kubernetes.client.namespace": namespace,
                "kubernetes.client.discovery.labels": [foo:"bar"]], Environment.KUBERNETES)
        KubernetesServiceInstanceEndpointProvider provider = applicationContext.getBean(KubernetesServiceInstanceEndpointProvider)

        when:
        def config = createConfig("example-client", true)
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 1

        when:
        config = createConfig("example-service", true)
        instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 2

        cleanup:
        applicationContext.close()
    }

    void "it doesn't fail when service endpoint has no ip addresses"() {
        given:
        Endpoints endpoints = operations.getClient(namespace).endpoints().create(new EndpointsBuilder()
                .withNewMetadata()
                .withName("empty-endpoint")
                .endMetadata()
                .build())

        when:
        def config = createConfig("empty-endpoint", true)
        def instanceList = Flowable.fromPublisher(provider.getInstances(config)).blockingFirst()

        then:
        instanceList.size() == 0
    }


    KubernetesServiceConfiguration createConfig(String name, manual = false){
        return new KubernetesServiceConfiguration(
                name,
                name,
                namespace,
                KubernetesServiceInstanceEndpointProvider.MODE,
                null,
                manual)
    }

}
