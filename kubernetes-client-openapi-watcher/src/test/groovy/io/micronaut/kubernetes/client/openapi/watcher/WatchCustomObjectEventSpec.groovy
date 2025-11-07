package io.micronaut.kubernetes.client.openapi.watcher

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.kubernetes.client.openapi.api.ApiextensionsV1Api
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.api.CustomObjectsApi
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinition
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinitionNames
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinitionSpec
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceDefinitionVersion
import io.micronaut.kubernetes.client.openapi.model.V1CustomResourceValidation
import io.micronaut.kubernetes.client.openapi.model.V1JSONSchemaProps
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.kubernetes.client.openapi.watcher.api.CustomObjectsApiWatcher
import io.micronaut.kubernetes.openapi.test.K3sContainerSpec
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.publisher.Flux
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ConcurrentHashMap

import static io.micronaut.kubernetes.openapi.test.KubernetesModels.getNamespaceModel
import static io.micronaut.kubernetes.openapi.test.KubernetesOperations.createNamespace

class WatchCustomObjectEventSpec extends K3sContainerSpec {

    private static final Logger LOG = LoggerFactory.getLogger(WatchEventsSpec)

    @Override
    Logger getLogger() {
        return LOG
    }

    @Inject
    ApiextensionsV1Api apiextensionsV1Api

    def 'watch custom resource events'() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "kubernetes.client.kube-config-path"   : "file:" + kubeConfigFile.toString()
        ], Environment.KUBERNETES)
        CustomObjectsApiWatcher apiWatcher = context.getBean(CustomObjectsApiWatcher.class)
        CustomObjectsApi customObjectsApi = context.getBean(CustomObjectsApi.class)
        CoreV1Api coreV1Api = context.getBean(CoreV1Api.class)
        ApiextensionsV1Api apiextensionsV1Api = context.getBean(ApiextensionsV1Api.class)

        when:
        def namespaceName = 'watch-custom-resource-test'
        def customObjectName = 'co-test-1'
        createNamespace(coreV1Api, getNamespaceModel(namespaceName))
        createCustomResourceDefinition(apiextensionsV1Api)
        def createdObject = createCustomObject(customObjectsApi, namespaceName, customObjectName)

        Map<String, List<String>> events = new ConcurrentHashMap<>()

        Flux<WatchEvent<Object>> flux = apiWatcher.listNamespacedCustomObject("test.io", "v1", namespaceName, "testcustoms", null, null, null, null, null, null, null, null, null, true)
        Disposable disposable = flux.subscribe(event -> {
            events.computeIfAbsent(event.object.metadata.name, key -> []).add(event.type)
        })

        replaceCustomObject(customObjectsApi, namespaceName, customObjectName, createdObject.metadata.resourceVersion)
        deleteCustomObject(customObjectsApi, namespaceName, customObjectName)

        PollingConditions conditions = new PollingConditions(timeout: 2)

        then:
        conditions.eventually {
            events.size() > 0
            events.get(customObjectName)?.get(0) == 'ADDED'
            events.get(customObjectName)?.get(1) == 'MODIFIED'
            events.get(customObjectName)?.get(2) == 'DELETED'
        }

        cleanup:
        disposable?.dispose()
    }

    private static void createCustomResourceDefinition(ApiextensionsV1Api apiextensionsV1Api) {
        V1JSONSchemaProps jsonSchemaProps = new V1JSONSchemaProps()
        jsonSchemaProps.setType("object")
        jsonSchemaProps.setProperties([
                apiVersion: getProp("string"),
                "kind"    : getProp("string"),
                "metadata": getProp("object"),
                "spec"    : getProp("object", ['test-property': getProp("string")])
        ])

        V1CustomResourceValidation customResourceValidation = new V1CustomResourceValidation()
        customResourceValidation.setOpenAPIV3Schema(jsonSchemaProps)

        V1CustomResourceDefinitionVersion customResourceDefinitionVersion = new V1CustomResourceDefinitionVersion("v1", true, true)
        customResourceDefinitionVersion.setSchema(customResourceValidation)

        V1CustomResourceDefinitionSpec customResourceDefinitionSpec = new V1CustomResourceDefinitionSpec(
                "test.io",
                new V1CustomResourceDefinitionNames("TestCustom", "testcustoms"),
                "Namespaced",
                [customResourceDefinitionVersion])

        V1CustomResourceDefinition customResourceDefinition = new V1CustomResourceDefinition(customResourceDefinitionSpec)
        V1ObjectMeta metadata = new V1ObjectMeta()
        metadata.setName("testcustoms.test.io")
        customResourceDefinition.setMetadata(metadata)

        apiextensionsV1Api.createCustomResourceDefinition(customResourceDefinition, null, null, null, null)
    }

    private static V1JSONSchemaProps getProp(String type, Map<String, V1JSONSchemaProps> props = [:]) {
        V1JSONSchemaProps specProp = new V1JSONSchemaProps()
        specProp.setType(type)
        specProp.setProperties(props)
        return specProp
    }

    private static Object createCustomObject(CustomObjectsApi customObjectsApi, String namespace, String name) {
        String body = '{"apiVersion":"test.io/v1","kind":"TestCustom","metadata":{"name":"' + name + '"}, "spec":{"test-property":"value1"}}'
        return customObjectsApi.createNamespacedCustomObject("test.io", "v1", namespace, "testcustoms", body, null, null, null, null)
    }

    private static Object replaceCustomObject(CustomObjectsApi customObjectsApi, String namespace, String name, String resourceVersion) {
        String body = '{"apiVersion":"test.io/v1","kind":"TestCustom","metadata":{"resourceVersion":"' + resourceVersion + '","name":"' + name + '"}, "spec":{"test-property":"value2"}}'
        return customObjectsApi.replaceNamespacedCustomObject("test.io", "v1", namespace, "testcustoms", name, body, null, null, null)
    }

    private static void deleteCustomObject(CustomObjectsApi customObjectsApi, String namespace, String name) {
        customObjectsApi.deleteNamespacedCustomObject("test.io", "v1", namespace, "testcustoms", name, null, null, null, null, null)
    }
}
