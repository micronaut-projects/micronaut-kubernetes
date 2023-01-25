package io.micronaut.kubernetes.client.rxjava2


import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.micronaut.context.annotation.Property
import io.micronaut.kubernetes.test.KubernetesSpecification
import io.micronaut.kubernetes.test.TestUtils
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Requires

import jakarta.inject.Inject

@MicronautTest
@Requires({ TestUtils.kubernetesApiAvailable() })
@Property(name = "kubernetes.client.namespace", value = "kubernetes-rx2-client")
@Property(name = "spec.reuseNamespace", value = "false")
class CoreV1ApiRxClientSpec extends KubernetesSpecification {

    @Inject
    CoreV1ApiRxClient apiRxClient

    @Override
    def setupFixture(String namespace) {
        createNamespaceSafe(namespace)

        operations.createConfigMap("config-map-1", namespace, [foo: 'bar'])
        operations.createConfigMap("config-map-2", namespace, [x: 'y'], [label: "red"])
    }

    def "list all config maps"() {
        when:
        V1ConfigMapList configMapList = apiRxClient.listNamespacedConfigMap(
                namespace,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null).blockingGet()

        then:
        configMapList
        !configMapList.items.isEmpty()
        configMapList.items.stream().filter(cm -> cm.metadata.name == "config-map-1").any()
        configMapList.items.stream().filter(cm -> cm.metadata.name == "config-map-2").any()
    }

    def "list all labeled config maps"() {
        when:
        V1ConfigMapList configMapList = apiRxClient.listNamespacedConfigMap(
                namespace,
                null,
                null,
                null,
                null,
                "label=red",
                null,
                null,
                null,
                null).blockingGet()

        then:
        configMapList
        !configMapList.items.isEmpty()
        configMapList.items.size() == 1
        configMapList.items.stream().filter(cm -> cm.metadata.name == "config-map-2").any()
    }
}
