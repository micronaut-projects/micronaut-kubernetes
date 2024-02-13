package io.micronaut.kubernetes.client.reactor

import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.context.annotation.Property
import io.micronaut.kubernetes.client.reactor.utils.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Requires

@MicronautTest
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "kubernetes-reactor-client")
@Property(name = "spec.reuseNamespace", value = "false")
class CoreV1ApiReactorClientSpec extends KubernetesSpecification {

    @Inject
    CoreV1ApiReactorClient apiReactorClient

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)

        operations.createConfigMap("config-map-1", namespace, [foo: 'bar'])
        operations.createConfigMap("config-map-2", namespace, [x: 'y'], [label: "red"])
    }

    def "list all config maps"() {
        when:
        V1ConfigMapList configMapList = apiReactorClient.listNamespacedConfigMap(
                namespace,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null).block()

        then:
        configMapList
        !configMapList.items.isEmpty()
        configMapList.items.stream().filter(cm -> cm.metadata.name == "config-map-1").any()
        configMapList.items.stream().filter(cm -> cm.metadata.name == "config-map-2").any()
    }

    def "list all labeled config maps"() {
        when:
        V1ConfigMapList configMapList = apiReactorClient.listNamespacedConfigMap(
                namespace,
                null,
                null,
                null,
                null,
                "label=red",
                null,
                null,
                null,
                null,
                null).block()

        then:
        configMapList
        !configMapList.items.isEmpty()
        configMapList.items.size() == 1
        configMapList.items.stream().filter(cm -> cm.metadata.name == "config-map-2").any()
    }
}
