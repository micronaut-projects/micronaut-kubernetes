package io.micronaut.kubernetes.client.reactor

import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.kubernetes.client.openapi.models.V1Namespace
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.micronaut.context.ApplicationContext
import io.micronaut.core.util.StringUtils
import io.micronaut.kubernetes.test.K3sContainerSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CoreV1ApiReactorClientSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(CoreV1ApiReactorClientSpec.class)

    private static final NAMESPACE_NAME = "micronaut-reactor-client"

    @Override
    Logger getLogger() {
        return LOG
    }

    @Override
    def setupKubernetes(ApplicationContext context) {
        def reactiveClient = context.getBean(CoreV1ApiReactorClient.class)
        reactiveClient.createNamespace(getNamespace(NAMESPACE_NAME))
                .execute()
                .block()
    }

    def "create and list config maps"() {
        given:
        ApplicationContext context = ApplicationContext.run(["kubernetes.client.kube-config-path": kubeConfigFile.toString()])
        def reactiveClient = context.getBean(CoreV1ApiReactorClient.class)
        def configMap1 = getConfigMap("config-map-1",
                ["data-key-1": "data-value-1", "data-key-2": "data-value-2"],
                ["label-key-1": "label-value-1"])
        def configMap2 = getConfigMap("config-map-2",
                ["data-key-3": "data-value-3"],
                ["label-key-3": "label-value-3"])

        when:
        def createdConfigMap1 = reactiveClient.createNamespacedConfigMap(NAMESPACE_NAME, configMap1)
                .execute()
                .block()
        def createdConfigMap2 = reactiveClient.createNamespacedConfigMap(NAMESPACE_NAME, configMap2)
                .execute()
                .block()

        then:
        StringUtils.isNotEmpty(createdConfigMap1.metadata.resourceVersion)
        StringUtils.isNotEmpty(createdConfigMap2.metadata.resourceVersion)

        when:
        def configMapList = reactiveClient.listNamespacedConfigMap(NAMESPACE_NAME)
                .execute()
                .block()

        then:
        configMapList.getItems().size() == 3
        findConfigMap(configMapList, "kube-root-ca.crt").isPresent()
        with (findConfigMap(configMapList, "config-map-1").get()) {
            it.data.get("data-key-1") == "data-value-1"
            it.data.get("data-key-2") == "data-value-2"
        }
        with (findConfigMap(configMapList, "config-map-2").get()) {
            it.data.get("data-key-3") == "data-value-3"
        }

        when:
        configMapList = reactiveClient.listNamespacedConfigMap(NAMESPACE_NAME)
                .labelSelector("label-key-1=label-value-1")
                .execute()
                .block()

        then:
        configMapList.getItems().size() == 1
        configMapList.getItems().get(0).getMetadata().getName() == "config-map-1"

        when:
        configMapList = reactiveClient.listNamespacedConfigMap(NAMESPACE_NAME)
                .fieldSelector("metadata.name=config-map-2")
                .execute()
                .block()

        then:
        configMapList.getItems().size() == 1
        configMapList.getItems().get(0).getMetadata().getName() == "config-map-2"

        cleanup:
        reactiveClient.deleteNamespacedConfigMap("config-map-1", NAMESPACE_NAME)
                .execute()
                .block()
        reactiveClient.deleteNamespacedConfigMap("config-map-2", NAMESPACE_NAME)
                .execute()
                .block()
    }

    Optional<V1ConfigMap> findConfigMap(V1ConfigMapList configMapList, String name) {
        return configMapList.items.stream().filter(cm -> cm.metadata.name == name).findFirst()
    }

    V1ObjectMeta getObjectMeta(String name, Map<String, String> labels = [:]) {
        V1ObjectMeta objectMeta = new V1ObjectMeta()
        objectMeta.name(name)
        objectMeta.labels(labels)
        return objectMeta
    }

    V1Namespace getNamespace(String name) {
        V1Namespace namespace = new V1Namespace()
        namespace.kind('Namespace')
        namespace.apiVersion('v1')
        namespace.metadata(getObjectMeta(name))
        return namespace
    }

    V1ConfigMap getConfigMap(String name, Map<String, String> data, Map<String, String> labels = [:]) {
        V1ConfigMap configMap = new V1ConfigMap()
        configMap.kind('ConfigMap')
        configMap.apiVersion('v1')
        configMap.metadata(getObjectMeta(name, labels))
        configMap.data(data)
        return configMap
    }
}
